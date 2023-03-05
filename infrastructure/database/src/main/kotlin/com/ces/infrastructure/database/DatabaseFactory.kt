package com.ces.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        Database.connect(hikari(config))
    }

    private fun hikari(config: DatabaseConfig): HikariDataSource {
        val hikari = HikariConfig()
        hikari.driverClassName = config.driver
        hikari.jdbcUrl = config.url
        hikari.username = config.user
        hikari.password = config.password
        hikari.maximumPoolSize = 3
        hikari.isAutoCommit = true
        hikari.validate()
        return HikariDataSource(hikari)
    }
}