package minun.zte.axon30.under_screen_adjustment;

import android.content.Context;

import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AdjustmentDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "adjustment";

    private static final int DATABASE_VERSION = 2;

    public AdjustmentDatabase(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table if not exists adjustment_preferences(" +
                       "name text, " +
                       "value text, " +
                       "time timestamp " +
                   ")");

        db.execSQL("create table if not exists adjustment_data(" +
                       "time timestamp, " +
                       "temperature float, " +
                       "brightness float, " +
                       "r float, " +
                       "g float, " +
                       "b float, " +
                       "a float, " +
                       "notch float " +
                   ")");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {
            case 1: {
                db.execSQL("alter table adjustment_data add column notch float");
                db.execSQL("update adjustment_data set notch = 0");
                break;
            }
        }

    }

    public void recordData(double time, float temperature, float brightness, float r, float g, float b, float a, float notch) {

        this.getWritableDatabase().execSQL(
                "insert into adjustment_data(time, temperature, brightness, r, g, b, a, notch) values(?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{time, temperature, brightness, r, g, b, a});

    }

    public List<AdjustmentRecord> listData(float limit) {

        List<AdjustmentRecord> list = new ArrayList<>();

        Cursor cursor = this.getWritableDatabase().rawQuery("select * from adjustment_data order by time desc limit ?", new String[]{"" + limit});
        try {
            while (cursor.moveToNext()) {
                AdjustmentRecord record = new AdjustmentRecord();
                record.time = cursor.getFloat(cursor.getColumnIndex("time"));
                record.brightness = cursor.getFloat(cursor.getColumnIndex("brightness"));
                record.temperature = cursor.getFloat(cursor.getColumnIndex("temperature"));
                record.r = cursor.getFloat(cursor.getColumnIndex("r"));
                record.g = cursor.getFloat(cursor.getColumnIndex("g"));
                record.b = cursor.getFloat(cursor.getColumnIndex("b"));
                record.a = cursor.getFloat(cursor.getColumnIndex("a"));
                record.notch = cursor.getFloat(cursor.getColumnIndex("notch"));
                list.add(record);
            }
        } finally {
            cursor.close();
        }

        return list;

    }

    public void clearData() {

        this.getWritableDatabase().execSQL(
                "delete from adjustment_data",
                new Object[]{});

    }

    public void updatePreference(String name, String value) {

        SQLiteDatabase database = this.getWritableDatabase();

        database.beginTransaction();

        try {
            Cursor cursor = database.rawQuery("select value from adjustment_preferences where name = ?", new String[]{name});
            boolean hasPreset = cursor.moveToNext();
            cursor.close();
            if (hasPreset) {
                database.execSQL("update adjustment_preferences set value = ?, time = ? where name = ?", new Object[]{value, new Date().getTime() / 1000f, name});
            } else {
                database.execSQL("insert into adjustment_preferences(name, time, value) values(?, ?, ?)", new Object[]{name, new Date().getTime() / 1000f, value});
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

    }

    public String getPreference(String name) {

        String result = null;

        Cursor cursor = this.getWritableDatabase().rawQuery("select value from adjustment_preferences where name = ?", new String[]{name});
        try {
            boolean hasPreset = cursor.moveToNext();
            if (hasPreset) {
                result = cursor.getString(cursor.getColumnIndex("value"));
            }
        } finally {
            cursor.close();
        }

        return result;

    }

}
