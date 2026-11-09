package com.hfad.odometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecorridoDbHelper extends SQLiteOpenHelper {

    public RecorridoDbHelper(Context context) {
        super(context, "tutorias.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE recorrido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "fecha TEXT, " +
                "metros REAL, " +
                "precision_m REAL, " +
                "intervalo_s INTEGER, " +
                "latitud REAL, " +
                "longitud REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS recorrido");
        onCreate(db);
    }

    public void guardar(double metros, float precision, int intervalo,
                        double lat, double lon) {
        ContentValues v = new ContentValues();
        v.put("fecha", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()));
        v.put("metros", metros);
        v.put("precision_m", precision);
        v.put("intervalo_s", intervalo);
        v.put("latitud", lat);
        v.put("longitud", lon);
        getWritableDatabase().insert("recorrido", null, v);
    }

    /** Reporte consolidado: sesiones y metros totales por día. */
    public String reporte() {
        StringBuilder sb = new StringBuilder();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT substr(fecha,1,10) AS d, COUNT(*), SUM(metros) " +
                        "FROM recorrido GROUP BY d ORDER BY d DESC", null);
        double total = 0;
        while (c.moveToNext()) {
            sb.append(String.format(Locale.getDefault(),
                    "%s | %d sesiones | %.1f m\n",
                    c.getString(0), c.getInt(1), c.getDouble(2)));
            total += c.getDouble(2);
        }
        c.close();
        sb.append(String.format(Locale.getDefault(), "TOTAL: %.1f m", total));
        return sb.toString();
    }
}