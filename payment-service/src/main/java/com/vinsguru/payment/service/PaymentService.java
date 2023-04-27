package com.vinsguru.payment.service;

import com.vinsguru.dto.PaymentDto;
import com.vinsguru.events.order.OrderEvent;
import com.vinsguru.events.payment.PaymentEvent;
import com.vinsguru.events.payment.PaymentStatus;
import com.vinsguru.payment.entity.UserTransaction;
import com.vinsguru.payment.repository.UserBalanceRepository;
import com.vinsguru.payment.repository.UserTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    @Autowired
    private UserBalanceRepository balanceRepository;

    @Autowired
    private UserTransactionRepository transactionRepository;

    @Transactional
    public PaymentEvent newOrderEvent(OrderEvent orderEvent){

    	System.out.println("HERE newOrderEvent......."+orderEvent);
    	
        var purchaseOrder = orderEvent.getPurchaseOrder();
        var dto = PaymentDto.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice());
        

    	System.out.println("HERE purchaseOrder.getUserId()......."+purchaseOrder.getUserId());
    	

    	System.out.println("HERE purchaseOrder.getPrice()......."+purchaseOrder.getPrice());
        
        PaymentEvent objPaymentEvent = this.balanceRepository.findById(purchaseOrder.getUserId())
                .filter(ub -> ub.getBalance() >= purchaseOrder.getPrice())
                .map(ub -> {
                    ub.setBalance(ub.getBalance() - purchaseOrder.getPrice());
                    this.transactionRepository.save(UserTransaction.of(purchaseOrder.getOrderId(), purchaseOrder.getUserId(), purchaseOrder.getPrice()));
                    return new PaymentEvent(dto, PaymentStatus.RESERVED);
                })
                .orElse(new PaymentEvent(dto, PaymentStatus.REJECTED));
        

    	System.out.println("HERE objPaymentEvent......."+objPaymentEvent);
        return objPaymentEvent;
    }

    @Transactional
    public void cancelOrderEvent(OrderEvent orderEvent){

    	System.out.println("HERE cancelOrderEvent......."+orderEvent);
        this.transactionRepository.findById(orderEvent.getPurchaseOrder().getOrderId())
                .ifPresent(ut -> {
                    this.transactionRepository.delete(ut);
                    this.balanceRepository.findById(ut.getUserId())
                            .ifPresent(ub -> ub.setBalance(ub.getBalance() + ut.getAmount()));
                });
    }
}
