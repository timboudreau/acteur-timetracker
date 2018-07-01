/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.timboudreau.trackerapi;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.errors.Err;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Description;
import com.mastfrog.acteur.preconditions.InjectRequestBodyAs;
import com.mastfrog.acteur.preconditions.MaximumPathLength;
import com.mastfrog.acteur.preconditions.MaximumRequestBodyLength;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.MinimumRequestBodyLength;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.preconditions.RequiredUrlParameters;
import com.mastfrog.acteur.util.PasswordHasher;
import com.mastfrog.util.time.TimeUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.bson.types.ObjectId;
import static com.timboudreau.trackerapi.Properties.*;
import static com.timboudreau.trackerapi.SignUpResource.MAX_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.SignUpResource.MIN_USERNAME_LENGTH;
import static com.timboudreau.trackerapi.Timetracker.USER_COLLECTION;
import java.time.ZonedDateTime;

/**
 *
 * @author Tim Boudreau
 */
@HttpCall
@PathRegex("^users/([^/]{" + MIN_USERNAME_LENGTH + "," + MAX_USERNAME_LENGTH + "}?)/signup$")
@MaximumPathLength(3)
@Methods({PUT, POST})
@RequiredUrlParameters("displayName")
@Description("New user signup")
@MinimumRequestBodyLength(SignUpResource.MIN_PASSWORD_LENGTH)
@MaximumRequestBodyLength(SignUpResource.MAX_PASSWORD_LENGTH)
@InjectRequestBodyAs(String.class)
class SignUpResource extends Acteur {

    public static final int MAX_PASSWORD_LENGTH = 40;
    public static final int MIN_PASSWORD_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 40;
    public static final int MIN_USERNAME_LENGTH = 3;

    @Inject
    SignUpResource(@Named(USER_COLLECTION) DBCollection coll, HttpEvent evt, PasswordHasher crypto, String password) throws UnsupportedEncodingException, IOException {
        String userName = evt.path().getElement(1).toString();
        DBObject existing = coll.findOne(new BasicDBObject(name, userName), new BasicDBObject(Properties._id, true));
        if (existing != null) {
            setState(new RespondWith(Err.conflict("A user named " + userName + " exists")));
            return;
        }
        DBObject nue = new BasicDBObject(name, new String[]{userName})
                .append(displayName, evt.urlParameter(displayName))
                .append(created, TimeUtil.toUnixTimestamp(ZonedDateTime.now()))
                .append(version, 0).append(authorizes, new ObjectId[0]);
        String encrypted = crypto.encryptPassword(password);
        nue.put(pass, encrypted);
        nue.put(origPass, encrypted);
        coll.save(nue, WriteConcern.FSYNCED);
        setState(new RespondWith(HttpResponseStatus.CREATED, nue.toMap()));
    }
}
