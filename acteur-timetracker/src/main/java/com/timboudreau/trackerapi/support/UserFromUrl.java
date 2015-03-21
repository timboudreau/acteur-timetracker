/*
 * The MIT License
 *
 * Copyright 2014 tim.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.timboudreau.trackerapi.support;

import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.mastfrog.acteur.HttpEvent;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.timboudreau.trackerapi.Properties;
import com.timboudreau.trackerapi.Timetracker;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author tim
 */
public class UserFromUrl implements Provider<TTUser> {

    private final Provider<DBCollection> usersCollection;
    private final Provider<HttpEvent> event;
    private final int position;

    public UserFromUrl(@Named(Timetracker.USER_COLLECTION) Provider<DBCollection> usersCollection, Provider<HttpEvent> event, int position) {
        this.usersCollection = usersCollection;
        this.event = event;
        this.position = position;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TTUser get() {
        HttpEvent evt = event.get();
        DBCollection users = usersCollection.get();
        String userName = evt.getPath().getElement(position).toString();
        DBObject u = users.findOne(new BasicDBObject("name", userName));
        if (u == null) {
            return null;
        }
        List<ObjectId> authorizes = (List<ObjectId>) u.get(Properties.authorizes);
        Number ver = (Number) u.get(Properties.version);
        int version = ver == null ? 0 : ver.intValue();
        return new TTUser(userName, (ObjectId) u.get("_id"), version, authorizes);
    }
}
