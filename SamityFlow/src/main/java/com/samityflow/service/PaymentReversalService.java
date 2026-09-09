package com.samityflow.service;


import com.samityflow.model.Payment;
import com.samityflow.pattern.command.ReversePaymentCommand;
import com.samityflow.repository.*;

import java.sql.Connection;



public class PaymentReversalService {



    private final Connection connection;




    public PaymentReversalService(
            Connection connection
    ) {

        this.connection = connection;

    }







    public void reversePayment(
            int paymentId
    ) {



        try {



            PaymentRepository paymentRepository =
                    new PaymentRepository(
                            connection
                    );



            Payment payment =
                    paymentRepository
                            .findById(paymentId)
                            .orElseThrow(
                                    () ->
                                    new RuntimeException(
                                            "Payment not found"
                                    )
                            );






            if(
                    "REVERSED"
                    .equals(payment.getStatus())
            ){


                throw new RuntimeException(
                        "Payment already reversed"
                );


            }





            ReversePaymentCommand command =
                    new ReversePaymentCommand(

                            payment,

                            paymentRepository,

                            new InstallmentRepository(
                                    connection
                            ),

                            new LoanRepository(
                                    connection
                            ),

                            new AuditLogRepository(
                                    connection
                            )

                    );





            command.execute();

        }
        catch(Exception e){



            throw new RuntimeException(
                    "Payment reversal failed",
                    e
            );


        }


    }


}