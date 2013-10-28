package com.melexis.viiper.route.finallotshipments;


import java.util.Map;

import com.melexis.foundation.util.IO;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;

public class FinalLotShipments extends RouteBuilder{

	// construct the query to detect if the shipment was last for that lot
	private Processor PrepareQueryFinalLotsShipped = new Processor()	{
		@Override
		public void process(Exchange exchange) throws Exception {
			final Message in = exchange.getIn();
			final String query = IO.resourceAsString(FinalLotShipments.class, "sql/finallotshipments.sql");
			final String evaluated = (String) new SimpleExpression(query).evaluate(exchange);
			in.setBody(evaluated);
		}
	};	

	// put the name of the lot in the body
	private Processor LotNameAsBody = new Processor()	{
		@Override	
		public void process(Exchange exchange) throws Exception {
			final Message in = exchange.getIn();
			final Map<String, String> row = in.getBody(Map.class);
			for (final Map.Entry<String, String> c : row.entrySet()) {
				in.setHeader(c.getKey(), c.getValue());
			}
			in.setBody(in.getHeader("LOTNAME", String.class));
		}
	};	
	@Override
	public void configure() throws Exception {

		//  define custom error handling
		errorHandler(
			deadLetterChannel("properties:{{finallotshipments.exceptions.to}}")
			.maximumRedeliveries(180)  
			.redeliveryDelay(60000)   // try to redeliver every 1 min
			.asyncDelayedRedelivery()
			.retryAttemptedLogLevel(LoggingLevel.WARN));
      
		//  Check the status of the lot shipped and report it if there are no quantities left 
		from("properties:{{customerdeliveries.from}}")
			.routeId("ViiperFinalLotShipments")
			.log("New Customer delivery: ${in.body} ")
			.process(PrepareQueryFinalLotsShipped)
			.to("jdbc:viiper-ds")					
			.split().body()
			.process(LotNameAsBody)
			.log("Submit lot \"${body}\" to Final Lot Shipments Topic.")
			.to("properties:{{finallotshipments.to}}");

	}
}
