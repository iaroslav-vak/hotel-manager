package com.vaka.repository;

import com.vaka.EntityProviderUtil;
import com.vaka.domain.Bill;
import com.vaka.util.ApplicationContext;

/**
 * Created by Iaroslav on 12/3/2016.
 */
public class BillRepositoryTest extends CrudRepositoryTest<Bill> {
    private BillRepository billRepository = ApplicationContext.getBean(BillRepository.class);

    @Override
    protected CrudRepository<Bill> getRepository() {
        return billRepository;
    }

    @Override
    protected Bill createEntity() {
        return EntityProviderUtil.createBill();
    }
}