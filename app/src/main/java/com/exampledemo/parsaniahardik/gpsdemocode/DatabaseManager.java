package com.exampledemo.parsaniahardik.gpsdemocode;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by igalk on 8/24/2017.
 */

interface DbRecord {

}

public class DatabaseManager extends HandlerThread {
    final private Handler mHandlerUi;
    private Handler mHandlerSelf;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDBRefHotzones;
    private DatabaseReference mDBRefUsers;
    private List<Query> mListQueries;

    public DatabaseManager(String name, Handler handlerUi) {
        super(name);

        mHandlerUi = handlerUi;
        mListQueries = new Vector<Query>();

        intializeDb();
    }

    private void intializeDb() {
        mDatabase = FirebaseDatabase.getInstance();

        initializeDbReferences();
        initializeQueries();
     }

    private void initializeQueries() {
        mListQueries.add(getQueryLastInsertedRecordByKey(mDBRefUsers, new UserLoggedInNotification(this)));
        mListQueries.add(getQueryLastInsertedRecordByKey(mDBRefHotzones, new HotZoneNotification(this)));
    }

    private Query getQueryLastInsertedRecordByKey(DatabaseReference db, ChildEventListener events) {
        long nTimeStampNow = System.currentTimeMillis();
        Query query = db.orderByChild("hashmapTimestamp/timestamp").limitToLast(1).startAt(nTimeStampNow);
        query.addChildEventListener(events);
        return query;
    }

    private void initializeDbReferences() {
        mDBRefHotzones =  mDatabase.getReference("/hotZones/");
        mDBRefUsers = mDatabase.getReference("/users/");
    }

    @Override
    public void onLooperPrepared() {
        mHandlerSelf = new Handler(getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                onMsg(msg);
            }
        };

        // not sure it's the right way
        Message msg = new Message();
        msg.what = ConstMessages.MSG_DB_HANDLER_CREATED;
        msg.obj = mHandlerSelf;
        mHandlerUi.sendMessage(msg);
    }

    public void SaveContent(final DbRecord obj) {
        if(obj instanceof HotZoneRecord) {
            mDBRefHotzones.orderByChild("mstrId").equalTo(((HotZoneRecord)obj).mstrId).limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    HotZoneRecord hotZoneRecord = (HotZoneRecord)obj;
                    if(dataSnapshot.hasChildren() == false){
                        mDBRefHotzones.child("").push().setValue(hotZoneRecord);
                    }
                    else{
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String key = snapshot.getKey();
                            mDBRefHotzones.child(key).setValue(hotZoneRecord);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else if (obj instanceof UserLoggedInRecord) {
            mDBRefUsers.child("").push().setValue((UserLoggedInRecord)obj);
        }
    }

    private void onMsg(Message msg) {
        switch(msg.what) {
            case ConstMessages.MSG_SAVE_NEW_RECORD:
                SaveContent((DbRecord)msg.obj);
        }
    }

    public Handler getHandler() {
        return mHandlerSelf;
    }

    public void onTableDataChange(DbRecord dbChangeInfo, int nEventType) {
        Message msg = mHandlerUi.obtainMessage();
        msg.what = nEventType;
        msg.obj = dbChangeInfo;
        mHandlerUi.sendMessage(msg);
    }
}

abstract class DatabaseRecordTimeStampInitializer {
    private HashMap<String, Object> mHashmapTimestamp;

    public DatabaseRecordTimeStampInitializer() {
        HashMap<String, Object> timestampNow = new HashMap<>();
        timestampNow.put("timestamp", ServerValue.TIMESTAMP);
        mHashmapTimestamp = timestampNow;
    }

    public HashMap<String, Object> getHashmapTimestamp(){
        return mHashmapTimestamp;
    }

    @Exclude
    public long getHashmapTimestampAsLong(){
        return (long)mHashmapTimestamp.get("timestamp");
    }
}

class UserLoggedInNotification
        implements ChildEventListener {

    private final DatabaseManager mdbMngr;

    public UserLoggedInNotification(DatabaseManager db) {
        super();
        mdbMngr = db;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            mdbMngr.onTableDataChange(dataSnapshot.getValue(UserLoggedInRecord.class)
                    , ConstMessages.MSG_DB_NEW_USER_CONNECTED);

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}

class HotZoneRecord extends DatabaseRecordTimeStampInitializer
        implements DbRecord {
    String mstrName;
    String mstrId;
    Double mdbLatitude;
    Double mdbLongtitude;

    public HotZoneRecord() {}

    public HotZoneRecord(
            String strName,
            String strId,
            Double dbLatitude,
            Double dbLongtitude) {
        super();
        mstrName = strName;
        mstrId = strId;
        mdbLatitude = dbLatitude;
        mdbLongtitude = dbLongtitude;
    }

    public String getMstrName() {
        return mstrName;
    }

    public String getMstrId() {
        return mstrId;
    }

    public double getMdbLatitude() {
        return mdbLatitude;
    }

    public double getMdbLongtitude() {
        return mdbLongtitude;
    }
}

class UserLoggedInRecord extends DatabaseRecordTimeStampInitializer
        implements DbRecord {
    String mstrName;
    String mstrId;
    String mstrType;

    public UserLoggedInRecord(String strName, String strId, String strType){
        super();
        mstrName = strName;
        mstrType = strType;
        mstrId = strId;
    }

    public UserLoggedInRecord() {}

    public String getMstrName() {
        return mstrName;
    }

    public String getMstrType() {
        return mstrType;
    }

    public String getMstrId() {
        return mstrId;
    }
}

class HotZoneNotification
        implements ChildEventListener {

    private final DatabaseManager mdbMngr;

    public HotZoneNotification (DatabaseManager db) {
        mdbMngr = db;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        mdbMngr.onTableDataChange(dataSnapshot.getValue(HotZoneRecord.class)
            , ConstMessages.MSG_NEW_HOT_ZONE_POINT_ARRIVED);

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
