/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */


import groovy.json.JsonBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.POST

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log
def uid = uid as Uid
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def json
if (objectClass.is(Neo4JUtils.RELATION)) {

    json = new JsonBuilder([
            statements: [
                    [
                            statement: "START r=relationship(${uid.uidValue}) DELETE r"
                    ]
            ]
    ])

} else {

    json = new JsonBuilder([
            statements: [
                    [
                            statement: "START n=node(${uid.uidValue}) OPTIONAL MATCH n-[r]-() DELETE r, n"
                    ]
            ]
    ])
}

if (log.ok) {
    log.ok("Transactional Cypher request {0}", json.toPrettyString())
}

connection.request(POST, JSON) { req ->
    uri.path = "/db/data/transaction/commit"
    body = json.toString()

    response.success = { HttpResponseDecorator resp ->
        return Neo4JUtils.parserResponse(resp, {})
    }

    response.failure = { HttpResponseDecorator resp ->
        throw new ConnectorException("REST POST failed with code:${resp.statusLine.statusCode} - ${resp.statusLine.reasonPhrase}")
    }
}
