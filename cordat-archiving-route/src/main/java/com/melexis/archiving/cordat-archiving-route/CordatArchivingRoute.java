package com.melexis.archiving.cordatarchivingroute;


import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;


public class CordatArchivingRoute extends RouteBuilder{
	
	
	@Override
	public void configure() throws Exception {
		
		errorHandler(
				deadLetterChannel("properties:{{exceptions.to}}")
				.maximumRedeliveries(1440) 
				.redeliveryDelay(60000)  
				.asyncDelayedRedelivery()
				.retryAttemptedLogLevel(LoggingLevel.WARN));

		from("properties:{{finallotshipments.from}}")
//			.routeId("CordatArchiver")
			.log("new lot for archiving ${in.body}")			
			.to("exec:/tmp/check?args=${in.body}");


	}

}
