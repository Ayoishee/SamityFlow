package com.samityflow.service;

import com.samityflow.model.Loan;
import com.samityflow.model.LoanApplication;
import com.samityflow.model.LoanProduct;

import com.samityflow.repository.AuditLogRepository;
import com.samityflow.repository.LoanApplicationRepository;
import com.samityflow.repository.LoanProductRepository;
import com.samityflow.repository.LoanRepository;

import com.samityflow.pattern.factory.ScheduleFactory;
import com.samityflow.pattern.strategy.CalculationStrategyFactory;
import com.samityflow.pattern.strategy.InterestStrategy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;


public class LoanDisbursementService {


    private final Connection connection;

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final LoanRepository loanRepository;
    private final AuditLogRepository auditLogRepository;

    private final ScheduleFactory scheduleFactory;



    public LoanDisbursementService(
            Connection connection,
            LoanApplicationRepository applicationRepository,
            LoanProductRepository productRepository,
            LoanRepository loanRepository,
            AuditLogRepository auditLogRepository,
            ScheduleFactory scheduleFactory
    ) {

        this.connection = connection;
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.loanRepository = loanRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleFactory = scheduleFactory;
    }



    public Loan disburse(int applicationId)
            throws SQLException {


        try {

            connection.setAutoCommit(false);



            // 1. Load application

            LoanApplication application =
                    applicationRepository
                            .findById(applicationId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Application not found"
                                    )
                            );



            // 2. Verify application status

            if (!"APPROVED".equals(
                    application.getStatus().name())) {

                throw new IllegalStateException(
                        "Only approved applications can be disbursed"
                );
            }



            // 3. Check duplicate loan

            if (loanRepository
                    .findByApplicationId(applicationId)
                    .isPresent()) {

                throw new IllegalStateException(
                        "Loan already exists for this application"
                );
            }



            // 4. Load loan product

            LoanProduct product =
                    productRepository
                            .findById(
                                    application.getProductId()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Loan product not found"
                                    )
                            );



            // 5. Select interest strategy

            InterestStrategy strategy =
                    CalculationStrategyFactory.interest(
                            product.interestStrategy()
                    );



            // 6. Calculate financial values

            BigDecimal principal =
                    BigDecimal.valueOf(
                            application.getAmount()
                    );


            double interestAmount =
                    strategy.calculate(
                            application.getAmount(),
                            product.interestRate(),
                            product.durationWeeks()
                    );


            BigDecimal interest =
                    BigDecimal.valueOf(
                            interestAmount
                    );


            BigDecimal totalPayable =
                    principal.add(
                            interest
                    );



            // 7. Create Loan

            Loan loan =
                    new Loan(
                            0,
                            applicationId,
                            application.getMemberId(),
                            principal.doubleValue()
                    );


            loan.setOutstanding(
                    totalPayable.doubleValue()
            );



            // 8. Save Loan

            loanRepository.save(
                    loan
            );



            // 9. Activate Loan

            loanRepository.updateStatus(
                    loan.getId(),
                    "ACTIVE"
            );



            // 10. Create Audit Log

            auditLogRepository.save(
                    "DISBURSE_LOAN",
                    "LOAN",
                    loan.getId(),
                    "SYSTEM",
                    "Loan successfully disbursed"
            );



            connection.commit();


            return loan;



        } catch(Exception e) {


            connection.rollback();

            throw e;


        } finally {


            connection.setAutoCommit(true);

        }

    }

}
