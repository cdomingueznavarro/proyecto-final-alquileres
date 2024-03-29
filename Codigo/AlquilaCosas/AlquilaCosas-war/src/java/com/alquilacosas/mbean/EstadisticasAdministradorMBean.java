/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.mbean;

import com.alquilacosas.common.EstadisticaAdminAlquiler;
import com.alquilacosas.common.EstadisticaAdminCategoria;
import com.alquilacosas.common.EstadisticaAdminPublicacion;
import com.alquilacosas.common.EstadisticaAdminUsuarios;
import com.alquilacosas.ejb.entity.EstadoAlquiler;
import com.alquilacosas.ejb.entity.EstadoUsuario;
import com.alquilacosas.ejb.session.EstadisticasAdministradorBeanLocal;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.apache.log4j.Logger;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.PieChartModel;

/**
 *
 * @author wilson
 */
@ManagedBean(name = "estadisticaAdmin")
@ViewScoped
public class EstadisticasAdministradorMBean implements Serializable {

    @EJB
    private EstadisticasAdministradorBeanLocal estadisticasAdministradorBean;
    private CartesianChartModel usuarioCantidadModel;
    private CartesianChartModel usuarioAcumuladoModel;
    private CartesianChartModel publicacionCantidadModel;
    private CartesianChartModel publicacionAcumuladoModel;
    private CartesianChartModel alquilerCantidadModel;
    private CartesianChartModel alquilerAcumuladoModel;
    private PieChartModel pieCategoriaModel;
    List<EstadisticaAdminUsuarios> listEstadisticaUsuario;
    List<EstadisticaAdminPublicacion> listEstadisticaPublicacion;
    List<EstadisticaAdminAlquiler> listEstadisticaAlquiler;
    List<EstadisticaAdminCategoria> listEstadisticaCategoria;
    private String anioMesCategorias;

    /**
     * Creates a new instance of EstadisticasAdministradorMBean
     */
    public EstadisticasAdministradorMBean() {
    }

    @PostConstruct
    public void init() {
        Logger.getLogger(EstadisticasAdministradorMBean.class).debug("EstadisticasAdministradorMBean: postconstruct.");
        cargarGraficosUsuario();
        cargarGraficosPublicacion();
        cargarGraficosAlquiler();
        //      !!! Por favor no descomentar este código, sino vuela todo !!! es para crear entidades aleatorias
        //crearUsuarios();
        //crearPublicaciones();
        //crearAlquileres();
        //this.crearDomicilios();
    }

    public void itemSelect(ItemSelectEvent event) {
        String anioMes = publicacionCantidadModel.getCategories().get(event.getItemIndex());
        anioMesCategorias = "Distribución de Categorias Publicadas del mes " + anioMes;
        listEstadisticaCategoria = estadisticasAdministradorBean.getEstadisticaAdminCategoria(anioMes);

        pieCategoriaModel.clear();
        for (EstadisticaAdminCategoria e : listEstadisticaCategoria) {
            pieCategoriaModel.set(e.getNombre(), e.getCantidad());
        }

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Categorias Publicadas", "Mes seleccionado: " + publicacionCantidadModel.getCategories().get(event.getItemIndex()));
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void cargarGraficosUsuario() {
        listEstadisticaUsuario = estadisticasAdministradorBean.getEstadisticaAdminUsuarios();

        usuarioCantidadModel = new CartesianChartModel();
        usuarioAcumuladoModel = new CartesianChartModel();
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM");

        ChartSeries cantRegistrados = new ChartSeries();
        ChartSeries acumRegistrados = new ChartSeries();
        cantRegistrados.setLabel("Registrados");
        acumRegistrados.setLabel("Registrados");
        for (EstadisticaAdminUsuarios e : listEstadisticaUsuario) {
            if (e.getEstado() == EstadoUsuario.NombreEstadoUsuario.REGISTRADO) {
                cantRegistrados.set(formato.format(e.getMes()), e.getCantidad());
                acumRegistrados.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        ChartSeries cantActivos = new ChartSeries();
        ChartSeries acumActivos = new ChartSeries();
        cantActivos.setLabel("Activados");
        acumActivos.setLabel("Activados");
        for (EstadisticaAdminUsuarios e : listEstadisticaUsuario) {
            if (e.getEstado() == EstadoUsuario.NombreEstadoUsuario.ACTIVO) {
                cantActivos.set(formato.format(e.getMes()), e.getCantidad());
                acumActivos.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        ChartSeries cantSuspendidos = new ChartSeries();
        ChartSeries acumSuspendidos = new ChartSeries();
        cantSuspendidos.setLabel("Suspendidos");
        acumSuspendidos.setLabel("Suspendidos");
        for (EstadisticaAdminUsuarios e : listEstadisticaUsuario) {
            if (e.getEstado() == EstadoUsuario.NombreEstadoUsuario.SUSPENDIDO) {
                cantSuspendidos.set(formato.format(e.getMes()), e.getCantidad());
                acumSuspendidos.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        usuarioCantidadModel.addSeries(cantRegistrados);
        usuarioCantidadModel.addSeries(cantActivos);
        usuarioCantidadModel.addSeries(cantSuspendidos);
        usuarioAcumuladoModel.addSeries(acumRegistrados);
        usuarioAcumuladoModel.addSeries(acumActivos);
        usuarioAcumuladoModel.addSeries(acumSuspendidos);
    }

    public void cargarGraficosPublicacion() {
        listEstadisticaPublicacion = estadisticasAdministradorBean.getEstadisticaAdminPublicaciones();

        publicacionCantidadModel = new CartesianChartModel();
        publicacionAcumuladoModel = new CartesianChartModel();
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM");

        ChartSeries cantRegistrados = new ChartSeries();
        ChartSeries acumRegistrados = new ChartSeries();
        cantRegistrados.setLabel("Registrados");
        acumRegistrados.setLabel("Registrados");
        for (EstadisticaAdminPublicacion e : listEstadisticaPublicacion) {
            cantRegistrados.set(formato.format(e.getMes()), e.getCantidad());
            acumRegistrados.set(formato.format(e.getMes()), e.getAcumulado());
        }

        publicacionCantidadModel.addSeries(cantRegistrados);
        publicacionAcumuladoModel.addSeries(acumRegistrados);

        pieCategoriaModel = new PieChartModel();
        Calendar fechaActual = Calendar.getInstance();
        String anioMes = formato.format(fechaActual.getTime());
        anioMesCategorias = "Distribución de Categorias Publicadas del mes " + anioMes;
        listEstadisticaCategoria = estadisticasAdministradorBean.getEstadisticaAdminCategoria(anioMes);

        pieCategoriaModel.clear();
        for (EstadisticaAdminCategoria e : listEstadisticaCategoria) {
            pieCategoriaModel.set(e.getNombre(), e.getCantidad());
        }
    }

    public void cargarGraficosAlquiler() {
        listEstadisticaAlquiler = estadisticasAdministradorBean.getEstadisticaAdminAlquiler();

        alquilerCantidadModel = new CartesianChartModel();
        alquilerAcumuladoModel = new CartesianChartModel();
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM");

        ChartSeries cantPedidos = new ChartSeries();
        ChartSeries acumPedidos = new ChartSeries();
        cantPedidos.setLabel("Pedidos");
        acumPedidos.setLabel("Pedidos");
        for (EstadisticaAdminAlquiler e : listEstadisticaAlquiler) {
            if (e.getEstado() == EstadoAlquiler.NombreEstadoAlquiler.PEDIDO) {
                cantPedidos.set(formato.format(e.getMes()), e.getCantidad());
                acumPedidos.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        ChartSeries cantConfirmados = new ChartSeries();
        ChartSeries acumConfirmados = new ChartSeries();
        cantConfirmados.setLabel("Confirmados");
        acumConfirmados.setLabel("Confirmados");
        for (EstadisticaAdminAlquiler e : listEstadisticaAlquiler) {
            if (e.getEstado() == EstadoAlquiler.NombreEstadoAlquiler.CONFIRMADO) {
                cantConfirmados.set(formato.format(e.getMes()), e.getCantidad());
                acumConfirmados.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        ChartSeries cantActivos = new ChartSeries();
        ChartSeries acumActivos = new ChartSeries();
        cantActivos.setLabel("Activos");
        acumActivos.setLabel("Activos");
        for (EstadisticaAdminAlquiler e : listEstadisticaAlquiler) {
            if (e.getEstado() == EstadoAlquiler.NombreEstadoAlquiler.ACTIVO) {
                cantActivos.set(formato.format(e.getMes()), e.getCantidad());
                acumActivos.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        ChartSeries cantFinalizados = new ChartSeries();
        ChartSeries acumFinalizados = new ChartSeries();
        cantFinalizados.setLabel("Finalizados");
        acumFinalizados.setLabel("Finalizados");
        for (EstadisticaAdminAlquiler e : listEstadisticaAlquiler) {
            if (e.getEstado() == EstadoAlquiler.NombreEstadoAlquiler.FINALIZADO) {
                cantFinalizados.set(formato.format(e.getMes()), e.getCantidad());
                acumFinalizados.set(formato.format(e.getMes()), e.getAcumulado());
            }
        }

        alquilerCantidadModel.addSeries(cantPedidos);
        alquilerCantidadModel.addSeries(cantConfirmados);
        alquilerCantidadModel.addSeries(cantActivos);
        alquilerCantidadModel.addSeries(cantFinalizados);
        alquilerAcumuladoModel.addSeries(acumPedidos);
        alquilerAcumuladoModel.addSeries(acumConfirmados);
        alquilerAcumuladoModel.addSeries(acumActivos);
        alquilerAcumuladoModel.addSeries(acumFinalizados);
    }

    public void crearUsuarios() {
        estadisticasAdministradorBean.crearUsuarios();
    }

    public void crearPublicaciones() {

        try {
            estadisticasAdministradorBean.crearPublicaciones();
        } catch (Exception e) {
            System.out.println("TODO MAL!!!!!");
        }

    }

    public void crearAlquileres() {
        estadisticasAdministradorBean.crearAlquileres();
    }
    
    public void crearDomicilios(){
        estadisticasAdministradorBean.crearDomicilios();
    }

    public CartesianChartModel getUsuarioCantidadModel() {
        return usuarioCantidadModel;
    }

    public void setUsuarioCantidadModel(CartesianChartModel usuarioCantidadModel) {
        this.usuarioCantidadModel = usuarioCantidadModel;
    }

    public CartesianChartModel getUsuarioAcumuladoModel() {
        return usuarioAcumuladoModel;
    }

    public void setUsuarioAcumuladoModel(CartesianChartModel usuarioAcumuladoModel) {
        this.usuarioAcumuladoModel = usuarioAcumuladoModel;
    }

    public CartesianChartModel getPublicacionAcumuladoModel() {
        return publicacionAcumuladoModel;
    }

    public void setPublicacionAcumuladoModel(CartesianChartModel publicacionAcumuladoModel) {
        this.publicacionAcumuladoModel = publicacionAcumuladoModel;
    }

    public CartesianChartModel getPublicacionCantidadModel() {
        return publicacionCantidadModel;
    }

    public void setPublicacionCantidadModel(CartesianChartModel publicacionCantidadModel) {
        this.publicacionCantidadModel = publicacionCantidadModel;
    }

    public CartesianChartModel getAlquilerAcumuladoModel() {
        return alquilerAcumuladoModel;
    }

    public void setAlquilerAcumuladoModel(CartesianChartModel alquilerAcumuladoModel) {
        this.alquilerAcumuladoModel = alquilerAcumuladoModel;
    }

    public CartesianChartModel getAlquilerCantidadModel() {
        return alquilerCantidadModel;
    }

    public void setAlquilerCantidadModel(CartesianChartModel alquilerCantidadModel) {
        this.alquilerCantidadModel = alquilerCantidadModel;
    }

    public List<EstadisticaAdminAlquiler> getListEstadisticaAlquiler() {
        return listEstadisticaAlquiler;
    }

    public void setListEstadisticaAlquiler(List<EstadisticaAdminAlquiler> listEstadisticaAlquiler) {
        this.listEstadisticaAlquiler = listEstadisticaAlquiler;
    }

    public List<EstadisticaAdminPublicacion> getListEstadisticaPublicacion() {
        return listEstadisticaPublicacion;
    }

    public void setListEstadisticaPublicacion(List<EstadisticaAdminPublicacion> listEstadisticaPublicacion) {
        this.listEstadisticaPublicacion = listEstadisticaPublicacion;
    }

    public List<EstadisticaAdminUsuarios> getListEstadisticaUsuario() {
        return listEstadisticaUsuario;
    }

    public void setListEstadisticaUsuario(List<EstadisticaAdminUsuarios> listEstadisticaUsuario) {
        this.listEstadisticaUsuario = listEstadisticaUsuario;
    }

    public List<EstadisticaAdminCategoria> getListEstadisticaCategoria() {
        return listEstadisticaCategoria;
    }

    public void setListEstadisticaCategoria(List<EstadisticaAdminCategoria> listEstadisticaCategoria) {
        this.listEstadisticaCategoria = listEstadisticaCategoria;
    }

    public PieChartModel getPieCategoriaModel() {
        return pieCategoriaModel;
    }

    public void setPieCategoriaModel(PieChartModel pieCategoriaModel) {
        this.pieCategoriaModel = pieCategoriaModel;
    }

    public String getAnioMesCategorias() {
        return anioMesCategorias;
    }

    public void setAnioMesCategorias(String anioMesCategorias) {
        this.anioMesCategorias = anioMesCategorias;
    }
}
