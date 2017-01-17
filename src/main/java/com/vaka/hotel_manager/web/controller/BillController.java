package com.vaka.hotel_manager.web.controller;

import com.vaka.hotel_manager.core.context.ApplicationContext;
import com.vaka.hotel_manager.domain.Bill;
import com.vaka.hotel_manager.domain.User;
import com.vaka.hotel_manager.service.BillService;
import com.vaka.hotel_manager.service.SecurityService;
import com.vaka.hotel_manager.util.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Iaroslav on 11/25/2016.
 */
public class BillController {
    private BillService billService;
    private SecurityService securityService;
    private static final Logger LOG = LoggerFactory.getLogger(BillController.class);

    public void getById(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("To bill page, loggedUser: {}", loggedUser);
        String strId = req.getRequestURI().split("/")[2];
        Integer id = Integer.valueOf(strId);
        Optional<Bill> bill = getBillService().getById(loggedUser, id);
        if (!bill.isPresent()) {
            throw new NotFoundException("There are no bill with given ID");
        }
        req.setAttribute("bill", bill.get());
        req.setAttribute("loggedUser", loggedUser);
        req.getRequestDispatcher("/billInfo.jsp").forward(req, resp);
    }

    public void getByReservationId(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("Getting bill by reservationId, loggedUser: {}", loggedUser);
        Integer id;
        try {
            id = Integer.valueOf(req.getParameter("id"));
        } catch (NumberFormatException e){
            throw new IllegalArgumentException("Reservation id must be integer value");
        }
        Optional<Bill> bill = getBillService().getBillByReservationId(loggedUser, id);
        if (!bill.isPresent())
            throw new NotFoundException("Not found");
        req.setAttribute("loggedUser", loggedUser);
        req.setAttribute("bill", bill.get());
        req.getRequestDispatcher("/billInfo.jsp").forward(req, resp);
    }

    public SecurityService getSecurityService() {
        if (securityService == null) {
            synchronized (this) {
                if (securityService == null) {
                    securityService = ApplicationContext.getInstance().getBean(SecurityService.class);
                }
            }
        }
        return securityService;
    }

    public BillService getBillService() {
        if (billService == null) {
            synchronized (this) {
                if (billService == null) {
                    billService = ApplicationContext.getInstance().getBean(BillService.class);
                }
            }
        }
        return billService;
    }
}
