package io.github.lburgazzoli.quarkus.ck;

import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorStateInfo;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Collection;

@Path("/kc")
public class ConnectResource {
    @Inject
    Herder herder;

    @Path("/connectors")
    @GET
    public Collection<String> list() {
        return herder.connectors();
    }

    @Path("/connectors/{id}/config")
    @GET
    public ConnectorInfo config(@PathParam("id") String id) {
        return herder.connectorInfo(id);
    }

    @Path("/connectors/{id}/status")
    @GET
    public ConnectorStateInfo status(@PathParam("id") String id) {
        return herder.connectorStatus(id);
    }
}
