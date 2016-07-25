package io.pivotal.quotes.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import io.pivotal.quotes.domain.CompanyInfo;
import io.pivotal.quotes.domain.Quote;
import io.pivotal.quotes.exception.SymbolNotFoundException;
import io.pivotal.quotes.service.QuoteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.HystrixCommand;


/**
 * Rest Controller providing the REST API for the Quote Service. Provides two
 * calls (both HTTP GET methods): - /quote/{symbol} - Retrieves the current
 * quote for a given symbol. - /company/{name} - Retrieves a list of company
 * information for companies that match the {name}.
 * 
 * @author David Ferreira Pinto
 * @author Reshmi Krishna
 */
@RestController
@RequestMapping(value = "/v1")
public class QuoteV1Controller {
	private static final Logger logger = LoggerFactory.getLogger(QuoteV1Controller.class);
	//including these
	private final TraceKeys traceKeys = new TraceKeys();


	/**
	 * The service to delegate calls to.
	 */
	@Autowired
	private QuoteService service;
	@Autowired
    Tracer tracer;

	/**
	 * Retrieves the current quote for the given symbol.
	 * 
	 * @param symbol
	 *            The symbol to retrieve the quote for.
	 * @return The Quote
	 * @throws SymbolNotFoundException
	 *             if the symbol is not valid.
	 */
	//TODO: should we leave this call here? we have the /quotes/
	/*@RequestMapping(value = "/quote/{symbol}", method = RequestMethod.GET)
	public ResponseEntity<Quote> getQuote(@PathVariable("symbol") final String symbol) throws SymbolNotFoundException {
		logger.debug("QuoteController.getQuote: retrieving quote for: " + symbol);
		Quote quote = service.getQuote(symbol);
		logger.info(String.format("Retrieved symbol: %s with quote %s", symbol, quote));
		return new ResponseEntity<Quote>(quote, getNoCacheHeaders(), HttpStatus.OK);
	}*/
	/**
	 * Retrieves the current quotes for the given symbols.
	 * 
	 * @param query
	 *            request parameter with q=symbol,symbol
	 * @return The Quote
	 * @throws SymbolNotFoundException
	 *             if the symbol is not valid.
	 */
	@RequestMapping(value = "/quotes", method = RequestMethod.GET)
	public ResponseEntity<List<Quote>> getQuotes(@RequestParam(value="q", required=false) String query) throws SymbolNotFoundException{
		logger.debug("received Quote query for: %s", query);
		if (query == null) {
			//return empty list.
			return new ResponseEntity<List<Quote>>(new ArrayList<Quote>(), getNoCacheHeaders(), HttpStatus.OK);
		}
		List<Quote> quotes;
		String[] splitQuery = query.split(",");
		if (splitQuery.length > 1) {
			quotes = service.getQuotes(query);
		} else {
			quotes = new ArrayList<>();
			String quote = splitQuery[0];
			quotes.add(service.getQuote(quote));
		}
		logger.info(String.format("Retrieved symbols: %s with quotes {}", query, quotes));
		return new ResponseEntity<List<Quote>>(quotes, getNoCacheHeaders(), HttpStatus.OK);
	}

	/**
	 * Searches for companies that have a name or symbol matching the parameter.
	 * 
	 * @param name
	 *            The name or symbol to search for.
	 * @return The list of companies that match the search parameter.
	 */
	//@HystrixCommand(fallbackMethod = "getBackupCompany")
	//@HystrixCommand(groupKey = "groupKey", commandKey = "commandKey", fallbackMethod = "getBackupCompany")
	@RequestMapping(value = "/company/{name}", method = RequestMethod.GET)
	public ResponseEntity<List<CompanyInfo>> getCompanies(@PathVariable("name") final String name) {
		logger.debug("QuoteController.getCompanies: retrieving companies for: " + name);
		List<CompanyInfo> companies = service.getCompanyInfo(name);
		logger.info(String.format("Retrieved companies with search parameter: %s - list: {}", name), companies);
		return new ResponseEntity<List<CompanyInfo>>(companies, HttpStatus.OK);
	}
	
//	public ResponseEntity<List<CompanyInfo>> getBackupCompany(@PathVariable("name") final String name) throws InterruptedException {
//        Span span = null;
//        try {
//            logger.debug("No companies available for : "+name+" Please check spelling");
//            logger.info("Unknown Symbol = "+name);
//            span = tracer.createSpan("processing_company_info");
//            span.logEvent("company_not_found");
//            tracer.addTag("quotes","failed");
//            tracer.addTag("symbol", name);
//            return null;
//        } finally {
//            tracer.close(span);
//        }
//
//    }
	
	@RequestMapping("/springonehystrix")
	public String springOneHystrix() throws Exception {
		return "HYSTRIX [" + new TraceCommand<String>(this.tracer, this.traceKeys,
				HystrixCommand.Setter.withGroupKey(
					HystrixCommandGroupKey.Factory.asKey("springone"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("springonecommandkey"))) {
			@Override public String doRun() throws Exception {
				return "hello_from_springone_hystrix";
			}
		}.execute() + "]";
	}


	/**
	 * Generates HttpHeaders that have the no-cache set.
	 * 
	 * @return HttpHeaders.
	 */
	private HttpHeaders getNoCacheHeaders() {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Cache-Control", "no-cache");
		return responseHeaders;
	}

	/**
	 * Handles the response to the client if there is any exception during the
	 * processing of HTTP requests.
	 * 
	 * @param e
	 *            The exception thrown during the processing of the request.
	 * @param response
	 *            The HttpResponse object.
	 * @throws IOException
	 */
	@ExceptionHandler({ Exception.class })
	public void handleException(Exception e, HttpServletResponse response) throws IOException {
		logger.warn("Handle Error: " + e.getMessage());
		logger.warn("Exception:", e);
		response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERROR: " + e.getMessage());
		// return "ERROR: " + e.getMessage();
	}
}
