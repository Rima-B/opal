package org.obiba.opal.web.magma.math;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static org.obiba.magma.math.summary.ContinuousVariableSummary.Distribution;

public interface ContinuousSummaryResource extends SummaryResource {

  @GET
  Response get(@QueryParam("d") @DefaultValue("normal") Distribution distribution,
      @QueryParam("p") List<Double> percentiles, @QueryParam("intervals") @DefaultValue("10") int intervals,
      @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit,
      @QueryParam("resetCache") @DefaultValue("false") boolean resetCache);

  @GET
  @Path("/cached-full-or-limit")
  Response getCachedFullOrComputeLimit(@QueryParam("d") @DefaultValue("normal") Distribution distribution,
      @QueryParam("p") List<Double> percentiles, @QueryParam("intervals") @DefaultValue("10") int intervals,
      @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit);
}
