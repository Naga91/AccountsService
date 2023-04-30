package com.nt.accounts.service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.nt.accounts.model.Cards;
import com.nt.accounts.model.Customer;


@FeignClient("cards")
public interface CardsFeignClient {
	
	@RequestMapping(method = RequestMethod.POST, value = "myCards", consumes="application/json")
	List<Cards> getCardDetails(@RequestHeader("nt-trace-id") String traceId, @RequestBody Customer customer);

}
