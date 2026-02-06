package com.safechestsx.storage

import com.safechestsx.Claim
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

data class ClaimRecord(
    val name: String,
    val owner: UUID,
    val trusted: Set<UUID>,
    val bypassed: Set<UUID>,
    val chestKeys: Set<String>
)

class ClaimsDatabase(plugin: JavaPlugin) {
    private val dbFile = File(plugin.dataFolder, "claims.db")
    private val jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"

    init {
        plugin.dataFolder.mkdirs()
        ensureSchema()
    }

    private fun connect(): Connection {
        val connection = DriverManager.getConnection(jdbcUrl)
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode=WAL")
            statement.execute("PRAGMA synchronous=NORMAL")
        }
        return connection
    }

    private fun ensureSchema() {
        connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS claims (
                        name TEXT PRIMARY KEY,
                        owner TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS trusted (
                        claim TEXT NOT NULL,
                        uuid TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS bypassed (
                        claim TEXT NOT NULL,
                        uuid TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS chests (
                        claim TEXT NOT NULL,
                        chest_key TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute("CREATE INDEX IF NOT EXISTS idx_trusted_claim ON trusted (claim)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_bypassed_claim ON bypassed (claim)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_chests_claim ON chests (claim)")
            }
        }
    }

    fun isEmpty(): Boolean {
        connect().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM claims").use { statement ->
                statement.executeQuery().use { result ->
                    return result.next() && result.getInt(1) == 0
                }
            }
        }
    }

    fun loadAll(): List<ClaimRecord> {
        val claims = mutableListOf<ClaimRecord>()
        connect().use { connection ->
            val trustedMap = loadUuidMap(connection, "trusted")
            val bypassedMap = loadUuidMap(connection, "bypassed")
            val chestMap = loadStringMap(connection, "chests", "chest_key")
            connection.prepareStatement("SELECT name, owner FROM claims").use { statement ->
                statement.executeQuery().use { result ->
                    while (result.next()) {
                        val name = result.getString("name")
                        val owner = UUID.fromString(result.getString("owner"))
                        claims.add(
                            ClaimRecord(
                                name = name,
                                owner = owner,
                                trusted = trustedMap[name] ?: emptySet(),
                                bypassed = bypassedMap[name] ?: emptySet(),
                                chestKeys = chestMap[name] ?: emptySet()
                            )
                        )
                    }
                }
            }
        }
        return claims
    }

    fun saveAll(claims: Collection<Claim>) {
        connect().use { connection ->
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                statement.execute("DELETE FROM claims")
                statement.execute("DELETE FROM trusted")
                statement.execute("DELETE FROM bypassed")
                statement.execute("DELETE FROM chests")
            }
            connection.prepareStatement("INSERT INTO claims (name, owner) VALUES (?, ?)").use { claimStmt ->
                connection.prepareStatement("INSERT INTO trusted (claim, uuid) VALUES (?, ?)").use { trustedStmt ->
                    connection.prepareStatement("INSERT INTO bypassed (claim, uuid) VALUES (?, ?)").use { bypassedStmt ->
                        connection.prepareStatement("INSERT INTO chests (claim, chest_key) VALUES (?, ?)").use { chestStmt ->
                            for (claim in claims) {
                                claimStmt.setString(1, claim.name)
                                claimStmt.setString(2, claim.owner.toString())
                                claimStmt.addBatch()
                                for (uuid in claim.trusted) {
                                    trustedStmt.setString(1, claim.name)
                                    trustedStmt.setString(2, uuid.toString())
                                    trustedStmt.addBatch()
                                }
                                for (uuid in claim.bypassed) {
                                    bypassedStmt.setString(1, claim.name)
                                    bypassedStmt.setString(2, uuid.toString())
                                    bypassedStmt.addBatch()
                                }
                                for (chest in claim.chestKeys) {
                                    chestStmt.setString(1, claim.name)
                                    chestStmt.setString(2, chest)
                                    chestStmt.addBatch()
                                }
                            }
                            claimStmt.executeBatch()
                            trustedStmt.executeBatch()
                            bypassedStmt.executeBatch()
                            chestStmt.executeBatch()
                        }
                    }
                }
            }
            connection.commit()
        }
    }

    private fun loadUuidMap(connection: Connection, table: String): Map<String, Set<UUID>> {
        val map = mutableMapOf<String, MutableSet<UUID>>()
        connection.prepareStatement("SELECT claim, uuid FROM $table").use { statement ->
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val claim = result.getString("claim")
                    val uuid = UUID.fromString(result.getString("uuid"))
                    map.computeIfAbsent(claim) { mutableSetOf() }.add(uuid)
                }
            }
        }
        return map
    }

    private fun loadStringMap(connection: Connection, table: String, valueColumn: String): Map<String, Set<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        connection.prepareStatement("SELECT claim, $valueColumn FROM $table").use { statement ->
            statement.executeQuery().use { result ->
                while (result.next()) {
                    val claim = result.getString("claim")
                    val value = result.getString(valueColumn)
                    map.computeIfAbsent(claim) { mutableSetOf() }.add(value)
                }
            }
        }
        return map
    }
}
