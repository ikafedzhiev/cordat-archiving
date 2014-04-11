package com.melexis.archiving.route.cordat;


import java.util.Map;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;



public class CordatArchivingDelaying extends RouteBuilder{
	
    private final String interval;

    public CordatArchivingDelaying(final String interval) {
				 this.interval = interval;
    }

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

		//	define custom error handling
		errorHandler(
			deadLetterChannel("properties:{{cordatarchiving.exceptions.to}}")
			.maximumRedeliveries(96) 
			.redeliveryDelay(900000) 
			.asyncDelayedRedelivery()
			.retryAttemptedLogLevel(LoggingLevel.WARN));
		
		//  Placing shipped lot in archive table 
		from("properties:{{cordatarchiver.delay.in.from}}")
			.routeId("CordatArchiverToDB")		
			.log("Delaying Cordat archiving for Lot: ${in.body}")
			.setHeader("lot", simple("${in.body}"))			
			.setBody(simple("INSERT INTO cordat (lot,date_of_last_shipment, archived) "
		 						+ " VALUES ('${in.header.lot}', CURRENT_TIMESTAMP , 'NO')"))	
		 	.to("jdbc:archive-pgsql-ds");
		
		// Check for lots which can be archived and do it.
		from("timer://kickoff?period=60m")
		  .routeId("CordatArchiverDelayChecker")	
		  .log("Looking for cordat lots for archiving which are are shipped: " + interval + " ago")		
		  .setBody(simple("SELECT lot as lotname "
		  						+ " FROM cordat "
		  						+ " WHERE date_of_last_shipment < now() - interval '" + interval + "'"
		  						+ " AND archived != 'YES'"))
		  .to("jdbc:archive-pgsql-ds")
		  .split().body()
		  .process(LotNameAsBody)
		  .log("Sending lot: ${in.body} to Delayed Archiving Topic")
		  .to("properties:{{cordatarchiver.delay.out.to}}")
		  .log("Marking lot: ${in.body} as archived in archive database.") 
		  .setBody(simple("UPDATE cordat SET archived='YES', date_of_archiving=now()"
		  						+ " WHERE lot='${in.body}'"
		  						+ " AND date_of_last_shipment < now() - interval '" + interval + "'"
		  						+ " AND archived != 'YES'"))
		  .to("jdbc:archive-pgsql-ds");		
     }

}


