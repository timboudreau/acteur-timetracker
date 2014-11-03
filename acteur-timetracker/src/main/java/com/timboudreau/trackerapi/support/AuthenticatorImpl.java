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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.mastfrog.acteur.auth.Authenticator;
import com.mastfrog.acteur.util.BasicCredentials;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.timboudreau.trackerapi.Properties;
import static com.timboudreau.trackerapi.Properties.name;
import static com.timboudreau.trackerapi.Properties.pass;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import java.io.IOException;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author tim
 */
public class AuthenticatorImpl implements Authenticator {

    private final Provider<DBCollection> coll;
    private final PasswordHasher crypto;

    @Inject
    AuthenticatorImpl(@Named(USER_COLLECTION) Provider<DBCollection> coll, PasswordHasher crypto) {
        this.coll = coll;
        this.crypto = crypto;
    }

    @Override
    public Object[] authenticate(String realm, BasicCredentials c) throws IOException {
        DBObject u = coll.get().findOne(new BasicDBObject(name, new String[]{c.username}));
        if (u == null) {
            u = coll.get().findOne(new BasicDBObject(name, c.username));
        }
        if (u == null) {
            return null;
        }
        String pw = (String) u.get(pass);
        if (pw == null) {
            return null;
        }
        if (!crypto.checkPassword(c.password, pw)) {
            return null;
        }
        List<ObjectId> authorizes = (List<ObjectId>) u.get(Properties.authorizes);
        Number ver = (Number) u.get(Properties.version);
        int version = ver == null ? 0 : ver.intValue();
        return new Object[]{new TTUser(c.username, (ObjectId) u.get("_id"), version, authorizes)};
    }
}
