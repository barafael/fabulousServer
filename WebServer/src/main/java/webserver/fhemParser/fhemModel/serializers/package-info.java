/**
 * This package contains custom serializers for Gson.
 * They are used to serialize a fhem model while also filtering out all information which a user
 * with given permissions should not be able to see.
 * This might seem like a crude approach but it avoids multithreading, deep copy and performance problems.
 *
 * @author Rafael on 11.07.17.
 */
package webserver.fhemParser.fhemModel.serializers;
