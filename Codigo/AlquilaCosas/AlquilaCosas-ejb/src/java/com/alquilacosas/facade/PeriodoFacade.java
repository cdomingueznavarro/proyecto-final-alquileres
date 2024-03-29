/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.facade;

import com.alquilacosas.ejb.entity.Periodo;
import com.alquilacosas.ejb.entity.Periodo.NombrePeriodo;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.log4j.Logger;

/**
 *
 * @author damiancardozo
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PeriodoFacade extends AbstractFacade<Periodo> {
    @PersistenceContext(unitName = "AlquilaCosas-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PeriodoFacade() {
        super(Periodo.class);
        
    }
    
    public List<Periodo> getPeriodos() {
        List<Periodo> periodos = null;
        Query query = em.createNamedQuery("Periodo.findAll");
        periodos = query.getResultList();
        return periodos;
    }
    
    public Periodo getPeriodo( int periodoId ) {
        
        Periodo p = em.find(Periodo.class, periodoId);
        return p;
        
    }
    
    public List<Periodo> getPeriodosOrderByHoras() {
        Query query = em.createNamedQuery("Periodo.findAll");
        return query.getResultList();   
    }
    
    public Periodo findByNombre(NombrePeriodo nombre) {
        Query query = em.createQuery("SELECT p FROM Periodo p WHERE p.nombre = :nombre");
        query.setParameter("nombre", nombre);
        Periodo periodo = null;
        try {
            periodo = (Periodo) query.getSingleResult();
        } catch (NoResultException e) {
            Logger.getLogger(PeriodoFacade.class).
                    error("findByNombre(). "
                    + "Excepcion al ejecutar consulta: " + e);
        }
        return periodo;
    }
    
}
