/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.mbean;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author damiancardozo
 */
@ManagedBean(name = "buscarBean")
@RequestScoped
public class BuscarMBean {

    private String buscar;
    
    /** Creates a new instance of BuscarMBean */
    public BuscarMBean() {
    }
    
    @PostConstruct
    public void init() {
        buscar = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("art");
    }

    public String buscar() {
        return "";
    }
    
    public String getBuscar() {
        return buscar;
    }

    public void setBuscar(String buscar) {
        this.buscar = buscar;
    }

}