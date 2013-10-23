package com.melexis.archiving.route.cordat;


import java.util.Arrays;
import java.util.List;


import org.apache.log4j.Logger;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.exec.ExecBinding;



public class CordatArchivingRoute extends RouteBuilder{
	
	private final static Logger log = Logger.getLogger(CordatArchivingRoute.class);
	
    class SetSite implements Processor {

        private String site;

        SetSite(String site) {
            this.site = site;
        }

        public void process(Exchange exchange) throws Exception {
        	final Message in = exchange.getIn();
        	List<String> args = Arrays.asList(site, in.getBody(String.class));
            in.setHeader("CamelExecCommandArgs", args);
        }
    }

	
	@Override
	public void configure() throws Exception {
		
		errorHandler(
			deadLetterChannel("properties:{{exceptions.to}}")
			.maximumRedeliveries(20) // 1440
			.redeliveryDelay(10000)    // 60000
			.asyncDelayedRedelivery()
			.retryAttemptedLogLevel(LoggingLevel.WARN));
		

		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverSofia")
			.log("Starting Cordat archiving for Lot: ${in.body} Site: SOFIA")
			.process(new SetSite("sofia"))
			.to("properties:{{cordatarchiver.to}}");

		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverIeper")
			.log("Starting Cordat archiving for Lot: ${in.body} Site: IEPER")
			.process(new SetSite("ieper"))
			.to("properties:{{cordatarchiver.to}}");

		from("properties:{{finallotshipments.from}}")
			.routeId("CordatArchiverErfurt")
			.log("Starting Cordat archiving for Lot: ${in.body} Site: ERFURT")
			.process(new SetSite("erfurt"))
			.to("properties:{{cordatarchiver.to}}")
			.process(new Processor() {
				public void process(Exchange exchange) throws Exception {
					String processOutput = (String) exchange.getIn().getBody(String.class); 
                    Integer exitCode = (Integer) exchange.getIn().getHeader(ExecBinding.EXEC_EXIT_VALUE, Integer.class); 
                    log.info(processOutput); 
                    log.info("Exit code: " + exitCode); 
				}
			});
     }
			

}
