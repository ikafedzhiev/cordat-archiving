package com.melexis.archiving.route.cordat;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.exec.*;


public class CordatArchivingRoute extends RouteBuilder{
	
	private final static Logger log = Logger.getLogger(CordatArchivingRoute.class);

    class ArchiveLot implements Processor {
    	
		private String site;

		public ArchiveLot(String site) {
            this.site = site;
		}

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

		errorHandler(
			deadLetterChannel("properties:{{exceptions.to}}")
			.maximumRedeliveries(4) // 1440
			.redeliveryDelay(60000)    // 60000
			.asyncDelayedRedelivery()
			.retryAttemptedLogLevel(LoggingLevel.WARN));
				
		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverSofia")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: SOFIA")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("sofia"));

		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverIeper")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: IEPER")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("ieper"));		

		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverErfurt")		
			.log("Starting Cordat archiving for Lot: ${in.body} Site: ERFURT")
			.setHeader("to", simple("properties:cordatarchiver.to"))
			.process(new ArchiveLot("erfurt"));

     }

}


