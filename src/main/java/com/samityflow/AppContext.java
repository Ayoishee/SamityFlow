package com.samityflow;

import com.samityflow.database.Database;
import com.samityflow.repository.*;
import com.samityflow.service.LoanApplicationService;
import com.samityflow.service.LoanCalculationService;

public class AppContext {
    public final Database database;
    public final UserRepository users;
    public final SamityRepository samities;
    public final GroupUnitRepository groups;
    public final MemberRepository members;
    public final LoanProductRepository products;
    public final LoanApplicationRepository applications;
    public final GuaranteeRepository guarantees;
    public final LoanApplicationService loanService;
    public final LoanCalculationService calculationService = new LoanCalculationService();

    public AppContext(Database database) {
        this.database = database;
        database.initialize();
        database.seed();
        users = new UserRepository(database);
        samities = new SamityRepository(database);
        groups = new GroupUnitRepository(database);
        members = new MemberRepository(database);
        products = new LoanProductRepository(database);
        applications = new LoanApplicationRepository(database);
        guarantees = new GuaranteeRepository(database);
        loanService = new LoanApplicationService(applications, members, products, guarantees);
    }
}
