package com.example.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import java.io.File

/**
 * Lược đồ quan hệ:
 *   DanhMuc(maDanhMuc PK, tenDanh)
 *   SuKien(maSuKien PK, moTa, tieuDe, diaDiem, ngayBatDau, gioBatDau, gioKetThuc,
 *          ngayKetThuc, thoiGianNhacNho, maDanhMuc FK -> DanhMuc)
 */
class DatabaseHelper private constructor(private val appContext: Context) :
    SQLiteOpenHelper(appContext, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "eventcal.db"
        private const val DB_VERSION = 1
        const val T_DANHMUC = "DanhMuc"
        const val T_SUKIEN = "SuKien"

        @Volatile private var instance: DatabaseHelper? = null
        fun get(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }

        private val DEFAULT_CATEGORIES = listOf(
            1L to "Công việc", 2L to "Cá nhân", 3L to "Gia đình",
            4L to "Bạn bè", 5L to "Sức khỏe", 6L to "Khác"
        )

        private const val SELECT =
            "SELECT s.maSuKien, s.tieuDe, s.moTa, s.diaDiem, s.ngayBatDau, s.gioBatDau, " +
                "s.ngayKetThuc, s.gioKetThuc, s.thoiGianNhacNho, s.maDanhMuc, IFNULL(d.tenDanh,'Khác') " +
                "FROM $T_SUKIEN s LEFT JOIN $T_DANHMUC d ON s.maDanhMuc = d.maDanhMuc"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T_DANHMUC (" +
                "maDanhMuc INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tenDanh TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE $T_SUKIEN (" +
                "maSuKien INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "moTa TEXT, " +
                "tieuDe TEXT NOT NULL, " +
                "diaDiem TEXT, " +
                "ngayBatDau TEXT NOT NULL, " +
                "gioBatDau TEXT NOT NULL, " +
                "gioKetThuc TEXT NOT NULL, " +
                "ngayKetThuc TEXT NOT NULL, " +
                "thoiGianNhacNho INTEGER NOT NULL DEFAULT 0, " +
                "maDanhMuc INTEGER, " +
                "FOREIGN KEY (maDanhMuc) REFERENCES $T_DANHMUC(maDanhMuc) ON DELETE SET NULL)"
        )
        db.execSQL("CREATE INDEX idx_sukien_ngay ON $T_SUKIEN(ngayBatDau, ngayKetThuc)")
        DEFAULT_CATEGORIES.forEach { (id, name) ->
            db.insert(T_DANHMUC, null, ContentValues().apply {
                put("maDanhMuc", id)
                put("tenDanh", name)
            })
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_SUKIEN")
        db.execSQL("DROP TABLE IF EXISTS $T_DANHMUC")
        onCreate(db)
    }

    // ------------------------------------------------------------------ DanhMuc
    fun getCategories(): List<Category> {
        val out = ArrayList<Category>()
        readableDatabase.rawQuery("SELECT maDanhMuc, tenDanh FROM $T_DANHMUC ORDER BY maDanhMuc", null)
            .use { c -> while (c.moveToNext()) out.add(Category(c.getLong(0), c.getString(1))) }
        return out
    }

    // ------------------------------------------------------------------ SuKien: CRUD
    private fun toValues(e: Event) = ContentValues().apply {
        put("tieuDe", e.title)
        put("moTa", e.description)
        put("diaDiem", e.location)
        put("ngayBatDau", e.startDate)
        put("gioBatDau", e.startTime)
        put("ngayKetThuc", e.endDate)
        put("gioKetThuc", e.endTime)
        put("thoiGianNhacNho", e.remindMinutes)
        put("maDanhMuc", e.categoryId)
    }

    fun insert(e: Event): Long = writableDatabase.insert(T_SUKIEN, null, toValues(e))

    fun update(e: Event): Int =
        writableDatabase.update(T_SUKIEN, toValues(e), "maSuKien = ?", arrayOf(e.id.toString()))

    fun delete(id: Long): Int =
        writableDatabase.delete(T_SUKIEN, "maSuKien = ?", arrayOf(id.toString()))

    fun deleteAll(): Int = writableDatabase.delete(T_SUKIEN, null, null)

    fun count(): Long = DatabaseUtils.queryNumEntries(readableDatabase, T_SUKIEN)

    // ------------------------------------------------------------------ SuKien: truy vấn
    private fun Cursor.toEvent() = Event(
        id = getLong(0),
        title = getString(1) ?: "",
        description = getString(2) ?: "",
        location = getString(3) ?: "",
        startDate = getString(4),
        startTime = getString(5),
        endDate = getString(6),
        endTime = getString(7),
        remindMinutes = getInt(8),
        categoryId = if (isNull(9)) 6L else getLong(9),
        categoryName = getString(10) ?: "Khác"
    )

    private fun query(
        where: String?, args: Array<String>?,
        order: String = "s.ngayBatDau, s.gioBatDau"
    ): List<Event> {
        val sql = SELECT + (if (where != null) " WHERE $where" else "") + " ORDER BY $order"
        val out = ArrayList<Event>()
        readableDatabase.rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(c.toEvent()) }
        return out
    }

    fun getEvent(id: Long): Event? =
        query("s.maSuKien = ?", arrayOf(id.toString())).firstOrNull()

    fun getAll(): List<Event> = query(null, null)

    /** Sự kiện diễn ra trong một ngày (kể cả sự kiện kéo dài nhiều ngày). */
    fun getByDate(date: String): List<Event> =
        query("s.ngayBatDau <= ? AND s.ngayKetThuc >= ?", arrayOf(date, date), "s.gioBatDau")

    /** Tập các ngày có sự kiện trong khoảng [rangeStart, rangeEnd] -> dùng vẽ chấm tròn. */
    fun getEventDates(rangeStart: String, rangeEnd: String): Set<String> {
        val set = HashSet<String>()
        query("s.ngayBatDau <= ? AND s.ngayKetThuc >= ?", arrayOf(rangeEnd, rangeStart)).forEach { e ->
            val from = maxOf(e.startDate, rangeStart)
            val to = minOf(e.endDate, rangeEnd)
            set.addAll(TimeUtils.datesBetween(from, to))
        }
        return set
    }

    /** Sự kiện chưa kết thúc (từ hôm nay trở đi), sắp xếp theo thời gian gần nhất. */
    fun getUpcoming(): List<Event> {
        val now = System.currentTimeMillis()
        return query("s.ngayKetThuc >= ?", arrayOf(TimeUtils.today()))
            .filter { !it.isEnded(now) }
            .sortedBy { it.startMillis }
    }

    /**
     * Tìm theo tiêu đề / mô tả, có thể lọc theo danh mục.
     * Danh mục lọc bằng SQL; chữ lọc bằng Kotlin vì LIKE của SQLite không phân biệt
     * hoa-thường với chữ có dấu tiếng Việt.
     */
    fun search(keyword: String, categoryId: Long?): List<Event> {
        val rows = if (categoryId == null) query(null, null)
        else query("s.maDanhMuc = ?", arrayOf(categoryId.toString()))
        val k = keyword.trim().lowercase()
        if (k.isEmpty()) return rows
        return rows.filter { it.title.lowercase().contains(k) || it.description.lowercase().contains(k) }
    }

    // ------------------------------------------------------------------ Sao lưu / khôi phục
    private fun dbFile(): File = appContext.getDatabasePath(DB_NAME)
    fun dbSizeKb(): Long = dbFile().length() / 1024

    fun exportTo(uri: Uri): Boolean = try {
        readableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
        appContext.contentResolver.openOutputStream(uri)?.use { out ->
            dbFile().inputStream().use { it.copyTo(out) }
        }
        true
    } catch (e: Exception) { false }

    fun importFrom(uri: Uri): Boolean {
        val tmp = File(appContext.cacheDir, "restore.tmp")
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return false
            // kiểm tra file hợp lệ trước khi thay thế
            SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { chk ->
                chk.rawQuery("SELECT COUNT(*) FROM $T_SUKIEN", null).use { it.moveToFirst() }
                chk.rawQuery("SELECT COUNT(*) FROM $T_DANHMUC", null).use { it.moveToFirst() }
            }
            close()
            File(dbFile().path + "-wal").delete()
            File(dbFile().path + "-shm").delete()
            tmp.copyTo(dbFile(), overwrite = true)
            true
        } catch (e: Exception) {
            false
        } finally {
            tmp.delete()
        }
    }
}
