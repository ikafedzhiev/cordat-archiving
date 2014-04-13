package com.melexis.archiving.route.cordat;

import java.util.Map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.exec.*;


public class CordatArchivingRoute extends RouteBuilder{

    private final String interval;

    public CordatArchivingRoute(final String interval) {
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
	
	private final static Logger log = Logger.getLogger(CordatArchivingRoute.class);

    class ArchiveLot implements Processor {
    	
		private String site;

		public ArchiveLot(String site) {
            this.site = site;
		}
		
		//  processor to call the cordat archiver and handle the exceptions
		public void process(Exchange exchange) throws Exception {
        	
			final Message in = exchange.getIn();

        	CommandLine cmdLine = new CommandLine((String) in.getHeader("to"));
         	cmdLine.addArgument((String) site);
        	cmdLine.addArgument(in.getBody(String.class));  
        	
        	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        	Executor executor = new DefaultExecutor();
        	PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);      
        	executor.setStreamHandler(streamHandler);        	
         	try {   
	        	executor.execute(cmdLine);
	        	log.info("Finished Cordat archiving for Lot: " + in.getBody(String.class) + " Site: " + site ); 

         	}  catch(Exception e) {
                throw new IOException("Error in cordat archiving Lot:  " + in.getBody(String.class)  
                		+ " Site: " + site + " Exception:" + e + outputStream.toString());
         	}
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

		//  Placing shipped lot in an archive table 
		from("properties:{{cordatarchiver.delay.in.from}}")
			.routeId("CordatArchiverToDB")		
			.log("Delaying Cordat archiving for Lot: ${in.body}")
			.setHeader("lot", simple("${in.body}"))			
			.setBody(simple("INSERT INTO cordat (id, lotname, date_of_last_shipment, archived) "
		 						+ " VALUES (nextval('cordat_id'), '${in.header.lot}', CURRENT_TIMESTAMP , False)"))	
		 	.to("jdbc:archive-pgsql-ds");
		
		// Check for lots which can be archived and do it.
		from("timer://kickoff?period=60m")
		  .routeId("CordatArchiverDelayChecker")	
		  .log("Looking for cordat lots for archiving which are are shipped: " + interval + " ago")		
		  .setBody(simple("SELECT lotname "
		  						+ " FROM cordat "
		  						+ " WHERE date_of_last_shipment < now() - interval '" + interval + "'"
		  						+ " AND archived != True"))
		  .to("jdbc:archive-pgsql-ds")
		  .split().body()
		  .process(LotNameAsBody)
		  .log("Sending lot: ${in.body} to Delayed Archiving Topic")
		  .to("properties:{{cordatarchiver.delay.out.to}}")
		  .log("Marking lot: ${in.body} as archived in archive database.") 
		  .setBody(simple("UPDATE cordat SET archived='YES', date_of_archiving=now()"
		  						+ " WHERE lotname='${in.body}'"
		  						+ " AND date_of_last_shipment < now() - interval '" + interval + "'"
		  						+ " AND archived != True"))
		  .to("jdbc:archive-pgsql-ds");		

		
		//  Cordat archiving route for SOFIA
		from("properties:{{cordatarchiver.sofia.from}}")
			.routeId("CordatArchiverSofia")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: SOFIA")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("sofia"));

		//  Cordat archiving route for IEPER
		from("properties:{{cordatarchiver.ieper.from}}")
			.routeId("CordatArchiverIeper")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: IEPER")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("ieper"));		

		//  Cordat archiving route for ERUFRT
		from("properties:{{cordatarchiver.erfurt.from}}")
			.routeId("CordatArchiverErfurt")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: ERFURT")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("erfurt"));

     }

}


