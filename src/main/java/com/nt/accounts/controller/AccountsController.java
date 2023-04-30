package com.nt.accounts.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nt.accounts.config.AccountsServiceConfig;
import com.nt.accounts.model.Accounts;
import com.nt.accounts.model.Cards;
import com.nt.accounts.model.Customer;
import com.nt.accounts.model.CustomerDetails;
import com.nt.accounts.model.Loans;
import com.nt.accounts.model.Properties;
import com.nt.accounts.repository.AccountsRepository;
import com.nt.accounts.service.client.CardsFeignClient;
import com.nt.accounts.service.client.LoansFeignClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@RestController
public class AccountsController {

	@Autowired
	AccountsRepository accountsRepository;

	@Autowired
	AccountsServiceConfig accountsConfig;

	@Autowired
	CardsFeignClient cardsFeignClient;

	@Autowired
	LoansFeignClient loansFeignClient;

	@PostMapping("/myAccount")
	public Accounts getAccountDetails(@RequestBody Customer customer) {
		Accounts accounts = this.accountsRepository.findByCustomerId(customer.getCustomerId());
		if (accounts != null) {
			return accounts;
		} else {
			return null;
		}
	}

	@GetMapping("/account/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Properties properties = new Properties(accountsConfig.getMsg(), accountsConfig.getBuildVersion(), 
				accountsConfig.getMailDetails(), accountsConfig.getActiveBranches());
		String response = ow.writeValueAsString(properties);
		return response;
	}

	@PostMapping("/myCustomerDetails")
//	@CircuitBreaker(name = "customerDetailsCB", fallbackMethod = "getCustomerDetailsFallBack")
	@Retry(name = "customerDetailsRetry", fallbackMethod = "getCustomerDetailsFallBack")
	public CustomerDetails getCustomerDetails(@RequestHeader("nt-trace-id") String traceId, @RequestBody Customer customer) {
		System.out.println("Inside Accounts service, traceId: " + traceId);
		Accounts accounts = this.accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = this.loansFeignClient.getLoansDetails(traceId, customer);
		List<Cards> cards = this.cardsFeignClient.getCardDetails(traceId, customer);
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		customerDetails.setCards(cards);
		return customerDetails;
	}

	
	private CustomerDetails getCustomerDetailsFallBack(String traceId, Customer customer, Throwable throwable) {
		Accounts accounts = this.accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = this.loansFeignClient.getLoansDetails(traceId, customer);
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		return customerDetails;
	}

	@GetMapping("/sayHello")
	@RateLimiter(name = "sayHello", fallbackMethod = "sayHelloFallBack")
	public String sayHello() {
		return "Hello, Welcome to NT Bank application";
	}
	
	private String sayHelloFallBack(Throwable throwable) {
		return "Hi, Welcome to NT Bank application";

	}
}
