package com.samityflow.service;

import com.samityflow.model.*;
import com.samityflow.pattern.chain.*;
import com.samityflow.repository.*;
import java.util.List;

/** Validates guarantees and advances applications using their agreed terms. */
public class LoanApplicationService {
    private final LoanApplicationRepository applications;
    private final MemberRepository members;
    private final LoanProductRepository products;
    private final GuaranteeRepository guarantees;
    private final EligibilityService eligibility;
    private final ApprovalHandler approvalChain;

    public LoanApplicationService(
            LoanApplicationRepository applications,
            MemberRepository members,
            LoanProductRepository products,
            GuaranteeRepository guarantees) {
        this.applications = applications;
        this.members = members;
        this.products = products;
        this.guarantees = guarantees;
        this.eligibility = new EligibilityService(members);

        ApprovalHandler first = new EligibilityHandler();
        first.setNext(new GuaranteeHandler())
                .setNext(new OfficerApprovalHandler())
                .setNext(new ManagerApprovalHandler());
        this.approvalChain = first;
    }

    /**
     * Validates an eligible member, positive amount, product and purpose; persists submission with
     * immutable terms. Zero guarantees advances directly to officer review. Invalid inputs throw
     * IllegalArgumentException or IllegalStateException. Repository connections remain
     * repository-owned.
     */
    public LoanApplication createAndSubmit(
            int memberId, int productId, double amount, String purpose) {
        Member member = requireMember(memberId);
        if (!eligibility.isEligible(member)) {
            throw new IllegalStateException("Member is not eligible to apply");
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Purpose is required");
        }
        products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        LoanApplication application =
                applications.saveNew(memberId, productId, amount, purpose.trim());
        application.submit();
        if (products.termsForApplication(application.getId()).requiredGuarantees() == 0) {
            application.guaranteesCompleted();
        }
        applications.update(application);
        return application;
    }

    /**
     * Adds one active same-group guarantor, rejecting self-guarantees, duplicates and invalid
     * application state. Advances when the agreed guarantee threshold is met.
     */
    public void addGuarantee(int applicationId, int guarantorMemberId) {
        LoanApplication application = requireApplication(applicationId);
        if (application.getStatus() != ApplicationStatus.GUARANTEE_PENDING) {
            throw new IllegalStateException("Application is not collecting guarantees");
        }
        if (application.getMemberId() == guarantorMemberId) {
            throw new IllegalArgumentException("Member cannot guarantee their own application");
        }
        Member applicant = requireMember(application.getMemberId());
        Member guarantor = requireMember(guarantorMemberId);
        if (applicant.groupUnitId() != guarantor.groupUnitId()) {
            throw new IllegalArgumentException("Guarantor must be from the same group");
        }
        if (!guarantor.active()) {
            throw new IllegalArgumentException("Guarantor must be active");
        }
        if (guarantees.exists(applicationId, guarantorMemberId)) {
            throw new IllegalArgumentException("This member already guaranteed the application");
        }

        guarantees.save(applicationId, guarantorMemberId);
        LoanProduct product = products.termsForApplication(application.getId());
        if (guarantees.countForApplication(applicationId) >= product.requiredGuarantees()) {
            application.guaranteesCompleted();
            applications.update(application);
        }
    }

    /**
     * Advances an application with the agreed guarantees to manager review; invalid workflow throws
     * IllegalStateException.
     */
    public void officerApprove(int applicationId) {
        LoanApplication application = requireApplication(applicationId);
        LoanProduct product = products.termsForApplication(application.getId());
        if (guarantees.countForApplication(applicationId) < product.requiredGuarantees()) {
            throw new IllegalStateException("Not enough guarantees");
        }
        application.officerApprove();
        applications.update(application);
    }

    /**
     * Checks eligibility, agreed guarantees and officer approval before recording manager approval.
     * Invalid workflow throws IllegalStateException.
     */
    public void managerApprove(int applicationId, String comment) {
        LoanApplication application = requireApplication(applicationId);
        Member member = requireMember(application.getMemberId());
        LoanProduct product = products.termsForApplication(application.getId());

        ApprovalRequest request =
                new ApprovalRequest(
                        eligibility.isEligible(member),
                        guarantees.countForApplication(applicationId),
                        product.requiredGuarantees(),
                        application.isOfficerApproved(),
                        true);
        approvalChain.handle(request);
        application.managerApprove(comment);
        applications.update(application);
    }

    /**
     * Records a required nonblank reason and rejects an application allowed by its current state.
     */
    public void reject(int applicationId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        LoanApplication application = requireApplication(applicationId);
        application.reject(reason.trim());
        applications.update(application);
    }

    /** Returns persisted applications ordered newest first. */
    public List<LoanApplication> findAll() {
        return applications.findAll();
    }

    private Member requireMember(int id) {
        return members.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }

    private LoanProduct requireProduct(int id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    private LoanApplication requireApplication(int id) {
        return applications
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
    }
}
