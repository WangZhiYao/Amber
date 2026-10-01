package cn.floriax.amber.core.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Upsert

/**
 * Common base of every DAO: the generic insert/upsert/delete set shared
 * by all tables. Specific DAOs extend it with their queries.
 *
 * Prefer [upsert] over [insert] unless the distinction matters: it
 * inserts a new row or updates the existing one in place, without the
 * delete-and-reinsert side effects of a REPLACE-strategy insert.
 *
 * @param T the entity (Row) type of the table.
 * @author WangZhiYao
 * @since 2026/9/30
 */
interface BaseDao<T> {

    /**
     * Inserts a row.
     *
     * @param item the row to insert.
     * @return the rowId of the newly inserted row (the ID), or -1 on
     * conflict without a REPLACE strategy.
     */
    @Insert
    suspend fun insert(item: T): Long

    /**
     * Inserts the row, or updates it when a row with the same primary
     * key already exists.
     *
     * @param item the row to insert or update.
     */
    @Upsert
    suspend fun upsert(item: T)

    /**
     * Deletes rows by entity (primary key match).
     *
     * @param item the row to delete.
     * @return the number of rows deleted.
     */
    @Delete
    suspend fun delete(item: T): Int
}
