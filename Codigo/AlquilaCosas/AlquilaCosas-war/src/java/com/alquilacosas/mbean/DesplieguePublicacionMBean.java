/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.mbean;

import com.alquilacosas.common.AlquilaCosasException;
import com.alquilacosas.dto.ComentarioDTO;
import com.alquilacosas.dto.PublicacionDTO;
import com.alquilacosas.ejb.entity.Periodo;
import com.alquilacosas.ejb.session.FavoritoBeanLocal;
import com.alquilacosas.ejb.session.PublicacionBeanLocal;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.primefaces.event.DateSelectEvent;
import org.primefaces.json.JSONObject;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;

/**
 *
 * @author jorge
 */
@ManagedBean(name = "publicacionDesplegada")
@ViewScoped
public class DesplieguePublicacionMBean implements Serializable {

    @EJB
    private PublicacionBeanLocal publicationBean;
    @ManagedProperty(value = "#{login}")
    private ManejadorUsuarioMBean usuarioLogueado;
    private PublicacionDTO publicacion;
    private String effect;
    private List<ComentarioDTO> comentarios;
    private String comentario;
    private String fecha_hasta;
    private List<Date> fechas;
    private String myJson;
    private Date fechaInicio;
    private int periodoAlquiler;
    private List<SelectItem> periodos;
    private int periodoSeleccionado;
    private Date today;
    private ComentarioDTO nuevaPregunta;
    private int cantidadProductos;
    private String action;
    private double userRating;
    private Date horaInicioAlquiler;
    private int denunciaId;
    private int motivoDenuncia;
    private boolean ownerLogged;
    private MapModel mapModel;
    List<Integer> idImagenes;
    private Integer publicacionId;
    @ManagedProperty(value = "#{misFavoritos}")
    private FavoritoMBean favorito;
    @EJB
    private FavoritoBeanLocal favoritoBean;
    private boolean addedToFavorito;

    /**
     * Creates a new instance of DesplieguePublicacionMBean
     */
    public DesplieguePublicacionMBean() {
    }

    @PostConstruct
    public void init() {
        Logger.getLogger(DesplieguePublicacionMBean.class).debug("DesplieguePublicacionMBean: postconstruct.");
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");

        if (id == null) {
            redirect();
            return;
        }

        try {
            publicacionId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            Logger.getLogger(DesplieguePublicacionMBean.class).error("Excepcion al parsear ID de parametro.");
            redirect();
            return;
        }

        publicacion = publicationBean.getPublicacion(publicacionId);
        cantidadProductos = publicacion.getCantidad();

        if (usuarioLogueado.getUsuarioId() != null && publicacionId != null) {
            if (favoritoBean.getFavorito(usuarioLogueado.getUsuarioId(), publicacionId) != null) {
                this.addedToFavorito = true;
            }
            ownerLogged = false;
            if (usuarioLogueado.getUsuarioId() == publicacion.getPropietario().getId()) {
                ownerLogged = true;
            }
        }

        setNuevaPregunta(new ComentarioDTO());
        setComentarios(publicationBean.getPreguntas(publicacionId));
        setMapModel(new DefaultMapModel());
        LatLng position = new LatLng(publicacion.getLatitud(), publicacion.getLongitud());
        getMapModel().addOverlay(new Marker(position, publicacion.getTitulo()));
        if (publicacion != null && publicacion.getFechaHasta() != null) {
            fecha_hasta = DateFormat.getDateInstance(DateFormat.SHORT).format(publicacion.getFechaHasta());
        }
        userRating = publicationBean.getUserRate(publicacion.getPropietario());
        myJson = "";
        cantidadProductos = 1;
        fechaInicio = new Date();
        effect = "fade";
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        today = cal.getTime();
        periodos = new ArrayList<SelectItem>();
        periodoAlquiler = 1;
        List<Periodo> listaPeriodos = publicationBean.getPeriodos();
        periodoSeleccionado = 2; //alto hardCode, para que por defecto este seleccionado dia y no hora (Jorge)
        for (Periodo periodo : listaPeriodos) {
            periodos.add(new SelectItem(periodo.getPeriodoId(), periodo.getNombre().name()));
        }
        idImagenes = publicacion.getImagenIds();
    }

    public void agregarFavorito() {
        if (!usuarioLogueado.isLogueado()) {
            RequestContext context = RequestContext.getCurrentInstance();
            context.addCallbackParam("noLogueado", true);
            context.execute("login.show();");
            return;
        } else if (usuarioLogueado.getUsuarioId() == publicacion.getPropietario().getId()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No puedes agregar un producto propio a favoritos.", ""));
            return;
        }

        try {
            Logger.getLogger(DesplieguePublicacionMBean.class).debug("Agregando un articulo a mis favoritos");
            PublicacionDTO pDto = publicationBean.getPublicacion(publicacionId);
            boolean agregado = favorito.agregarFavorito(usuarioLogueado.getUsuarioId(), pDto);
            if (agregado) {
                addedToFavorito = true;
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Publicación agregada a Mis Favoritos", "");
                FacesContext.getCurrentInstance().addMessage(null, msg);
            }
        } catch (Exception e) {
            Logger.getLogger(DesplieguePublicacionMBean.class).error("No se pudo agregar el artículo a Mis Favoritos");
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al agregar favorito", e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void redirect() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("inicio.jsf");
        } catch (Exception e) {
            Logger.getLogger(DesplieguePublicacionMBean.class).error("Excepcion al ejecutar redirect().");
        }
    }

    public void actualizarPreguntas() {
        comentarios = publicationBean.getPreguntas(publicacion.getId());

    }

    public void actualizarFechas() {
        boolean disponibilidad = true;
        if (cantidadProductos > publicacion.getCantidad()) {
            disponibilidad = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "La disponibilidad máxima es de " + publicacion.getCantidad() + " producto/s", ""));
        } else {
            try {
                fechas = publicationBean.getFechasSinStock(publicacion.getId(), cantidadProductos);
                createDictionary();
            } catch (AlquilaCosasException e) {
                Logger.getLogger(DesplieguePublicacionMBean.class).error("Excepcion al ejecutar getFechasSinStock(): " + e.getMessage());
            }
        }

        boolean logueado = usuarioLogueado.isLogueado();
        if (logueado) {
            if (usuarioLogueado.getUsuarioId() == publicacion.getPropietario().getId()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Usted no puede alquilar su propio producto", ""));
                ownerLogged = true;
            }
        }
        RequestContext context = RequestContext.getCurrentInstance();
        context.addCallbackParam("logueado", logueado);
        context.addCallbackParam("hayDisponibilidad", disponibilidad);
        context.addCallbackParam("ownerLogged", ownerLogged);
        action = "alquilar";
    }

    public void preguntar() {
        boolean logueado = usuarioLogueado.isLogueado();
        if (logueado) {
            if (usuarioLogueado.getUsuarioId() == publicacion.getPropietario().getId()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Usted no puede realizar preguntas en su propio producto", ""));
                ownerLogged = true;
            }
        }
        RequestContext context = RequestContext.getCurrentInstance();
        context.addCallbackParam("logueado", logueado);
        context.addCallbackParam("ownerLogged", ownerLogged);
        action = "preguntar";
    }

    public String guardarPregunta() {
        getNuevaPregunta().setUsuarioId(usuarioLogueado.getUsuarioId());
        getNuevaPregunta().setUsuario(usuarioLogueado.getUsername());
        getNuevaPregunta().setFecha(new Date());
        try {
            publicationBean.setPregunta(publicacion.getId(), getNuevaPregunta());
            setNuevaPregunta(new ComentarioDTO());
            //actualizarPreguntas();
        } catch (AlquilaCosasException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al enviar pregunta", e.getMessage()));
        }
        return null;
    }

    public String confirmarPedido() {
        String redireccion = null;
        if (usuarioLogueado.getUsuarioId() == publicacion.getPropietario().getId()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No esta permitido alquilar sus propios productos", ""));
        } else {
            Calendar beginDate = Calendar.getInstance();
            beginDate.setTime(fechaInicio);
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(fechaInicio);

            switch (periodoSeleccionado) {
                case 1: //horas

                    try {
                        Calendar time = Calendar.getInstance();
                        time.setTime(horaInicioAlquiler);
                        beginDate.add(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
                        beginDate.add(Calendar.MINUTE, time.get(Calendar.MINUTE));
                        endDate.add(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
                        endDate.add(Calendar.MINUTE, time.get(Calendar.MINUTE));

                    } catch (Exception e) {
                        FacesContext.getCurrentInstance().addMessage(null,
                                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Ingrese una hora de inicio valida", ""));
                        return null;
                    }
                    endDate.add(Calendar.HOUR_OF_DAY, periodoAlquiler);
                    break;
                case 2: //dias
                    endDate.add(Calendar.DATE, periodoAlquiler);
                    break;
                case 3: //semanas
                    endDate.add(Calendar.WEEK_OF_YEAR, periodoAlquiler);
                    break;
                case 4: //meses
                    endDate.add(Calendar.MONTH, periodoAlquiler);
            }

            long minDuration = calcularDuracion(
                    publicacion.getPeriodoMinimo().getPeriodoId(),
                    publicacion.getPeriodoMinimoValor());

            long maxDuration;
            try {
                maxDuration = calcularDuracion(
                        publicacion.getPeriodoMaximo().getPeriodoId(),
                        publicacion.getPeriodoMaximoValor());
            } catch (NullPointerException e) {
                //si no hay un periodo maximo, le pongo el mayor valor posible.
                maxDuration = Long.MAX_VALUE;
            }

            long periodoAlquilerEnMillis = endDate.getTimeInMillis() - beginDate.getTimeInMillis();
            if (periodoAlquilerEnMillis < minDuration || periodoAlquilerEnMillis > maxDuration) {
                StringBuilder mensaje = new StringBuilder();
                mensaje.append("El periodo minimo de alquiler es de ");
                mensaje.append(publicacion.getPeriodoMinimoValor());
                mensaje.append(" ");
                mensaje.append(publicacion.getPeriodoMinimo().getNombre());
                mensaje.append("/s ");
                if (maxDuration != Long.MAX_VALUE) {
                    mensaje.append("y el maximo de ");
                    mensaje.append(publicacion.getPeriodoMaximoValor());
                    mensaje.append(" ");
                    mensaje.append(publicacion.getPeriodoMaximo().getNombre());
                    mensaje.append("/s ");
                }
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        mensaje.toString(), ""));
            } else {
                Iterator<Date> itFechasSinStock = fechas.iterator();
                boolean noStockFlag = false;
                Calendar temp = Calendar.getInstance();
                //Recorro la lista de fechas sin stock fijandome si alguna cae
                //en el periodo seleccionado
                while (!noStockFlag && itFechasSinStock.hasNext()) {
                    temp.setTime(itFechasSinStock.next());
                    if (beginDate.before(temp) && endDate.after(temp))//la fecha sin stock cae en el periodo
                    {
                        noStockFlag = true;
                    }
                }
                if (noStockFlag) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Hay fechas sin stock en el periodo seleccionado", ""));
                } else {
                    //calculo el monto
                    double monto = calcularMonto(beginDate, endDate);


                    try {
                        publicationBean.crearPedidoAlquiler(publicacion.getId(),
                                usuarioLogueado.getUsuarioId(), beginDate.getTime(),
                                endDate.getTime(), monto, cantidadProductos);
                        redireccion = "/vistas/usuario/articulos?faces-redirect=true";
                    } catch (AlquilaCosasException ex) {
                        FacesContext.getCurrentInstance().addMessage(null,
                                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "El pedido de alquiler no se ha concretado, por favor intente de nuevo", ""));
                        Logger.getLogger(DesplieguePublicacionMBean.class).error("Excepcion al crear pedido de alquiler: " + ex + ": " + ex.getMessage());
                        redireccion = null;
                    }

                }
            }
        }
        return redireccion;
    }

    private double calcularMonto(Calendar beginDate, Calendar endDate) {
        double monto = 0;
        Calendar temp = Calendar.getInstance();
        temp.setTime(beginDate.getTime());
        temp.add(Calendar.MONTH, 1);
        endDate.add(Calendar.SECOND, 1);
        int second = endDate.get(Calendar.SECOND);
        while (endDate.after(temp)) {
            //hardCode muy duro, se el orden porque hice la consulta con orderby
            //si se agregan periodos nuevos corregir esto
            monto += publicacion.getPrecios().get(3).getPrecio();
            temp.add(Calendar.MONTH, 1);
        }

        temp.add(Calendar.MONTH, -1);
        temp.add(Calendar.WEEK_OF_YEAR, 1);
        while (endDate.after(temp)) {
            //hardCode muy duro, se el orden porque hice la consulta con orderby
            //si se agregan periodos nuevos corregir esto
            monto += publicacion.getPrecios().get(2).getPrecio();
            temp.add(Calendar.WEEK_OF_YEAR, 1);


        }

        temp.add(Calendar.WEEK_OF_YEAR, -1);
        temp.add(Calendar.DATE, 1);
        while (endDate.after(temp)) {
            //hardCode muy duro, se el orden porque hice la consulta con orderby
            //si se agregan periodos nuevos corregir esto
            monto += publicacion.getPrecios().get(1).getPrecio();
            temp.add(Calendar.DATE, 1);
        }

        temp.add(Calendar.DATE, -1);
        temp.add(Calendar.HOUR_OF_DAY, 1);
        while (endDate.after(temp)) {
            //hardCode muy duro, se el orden porque hice la consulta con orderby
            //si se agregan periodos nuevos corregir esto
            monto += publicacion.getPrecios().get(0).getPrecio();
            temp.add(Calendar.HOUR_OF_DAY, 1);
        }
        endDate.add(Calendar.SECOND, -1);
        return monto;
    }

    private long calcularDuracion(int periodoId, long periodo) {
        switch (periodoId) {
            case 1: //horas
                periodo *= 60 * 60 * 1000;
                break;
            case 2: //dias
                periodo *= 24 * 60 * 60 * 1000;
                break;
            case 3: //semanas
                periodo *= 7 * 24 * 60 * 60 * 1000;
                break;
            case 4: //meses
                Date now = new Date();
                Calendar temp = Calendar.getInstance();
                temp.setTime(now);
                temp.add(Calendar.MONTH, (int) periodo);
                periodo = temp.getTimeInMillis() - now.getTime();
        }
        return periodo;
    }

    private void createDictionary() {
        try {
            int month = -1;
            JSONObject yearJson = new JSONObject();
            JSONObject monthJson = new JSONObject();
            JSONObject dayJson = null;
            Calendar cal = Calendar.getInstance();
            for (Date d : fechas) {
                cal.setTime(d);
                if (cal.get(Calendar.MONTH) + 1 != month) {
                    if (dayJson != null) {
                        monthJson.putOpt(Integer.toString(month), dayJson);
                    }
                    dayJson = new JSONObject();
                    month = cal.get(Calendar.MONTH) + 1;
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    dayJson.putOpt(Integer.toString(day), true);
                } else {
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    dayJson.putOpt(Integer.toString(day), true);
                }
            }
            if (dayJson != null) {
                monthJson.putOpt(Integer.toString(month), dayJson);
            }
            int y = cal.get(Calendar.YEAR);
            yearJson.putOpt(Integer.toString(y), monthJson);
            myJson = yearJson.toString();
        } catch (Exception e) {
            Logger.getLogger(DesplieguePublicacionMBean.class).error("Exception creating JSON dictionary: " + e);
            System.out.println("error armando json: " + e);
        }
    }

    public String getFechas() {
        return myJson;
    }

    public String seleccionarFecha(DateSelectEvent e) {
        fechaInicio = e.getDate();
        return "";
    }

    /**
     * @return the comentarios
     */
    public List<ComentarioDTO> getComentarios() {
        return comentarios;
    }

    /**
     * @param comentarios the comentarios to set
     */
    public void setComentarios(List<ComentarioDTO> comentarios) {
        this.comentarios = comentarios;
    }

    /**
     * @return the publicacion
     */
    public PublicacionDTO getPublicacion() {
        return publicacion;
    }

    /**
     * @param publicacion the publicacion to set
     */
    public void setPublicacion(PublicacionDTO publicacion) {
        this.publicacion = publicacion;
    }

    /**
     * @return the effect
     */
    public String getEffect() {
        return effect;
    }

    /**
     * @param effect the effect to set
     */
    public void setEffect(String effect) {
        this.effect = effect;
    }

    /**
     * @return the usuarioLogueado
     */
    public ManejadorUsuarioMBean getUsuarioLogueado() {
        return usuarioLogueado;
    }

    /**
     * @param usuarioLogueado the usuarioLogueado to set
     */
    public void setUsuarioLogueado(ManejadorUsuarioMBean usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;
    }

    /**
     * @return the fecha_hasta
     */
    public String getFecha_hasta() {
        return fecha_hasta;
    }

    /**
     * @param fecha_hasta the fecha_hasta to set
     */
    public void setFecha_hasta(String fecha_hasta) {
        this.fecha_hasta = fecha_hasta;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    /**
     * @return the fechaInicio
     */
    public Date getFechaInicio() {
        return fechaInicio;
    }

    /**
     * @param fechaInicio the fechaInicio to set
     */
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    /**
     * @return the periodoAlquiler
     */
    public int getPeriodoAlquiler() {
        return periodoAlquiler;
    }

    /**
     * @param periodoAlquiler the periodoAlquiler to set
     */
    public void setPeriodoAlquiler(int periodoAlquiler) {
        this.periodoAlquiler = periodoAlquiler;
    }

    /**
     * @return the periodos
     */
    public List<SelectItem> getPeriodos() {
        return periodos;
    }

    /**
     * @param periodos the periodos to set
     */
    public void setPeriodos(List<SelectItem> periodos) {
        this.periodos = periodos;
    }

    /**
     * @return the periodoSeleccionado
     */
    public int getPeriodoSeleccionado() {
        return periodoSeleccionado;
    }

    /**
     * @param periodoSeleccionado the periodoSeleccionado to set
     */
    public void setPeriodoSeleccionado(int periodoSeleccionado) {
        this.periodoSeleccionado = periodoSeleccionado;
        //RequestContext context = RequestContext.getCurrentInstance();
        //context.addCallbackParam("mostrarHoraInicio", (periodoSeleccionado == 1));
    }

    /**
     * @return the today
     */
    public Date getToday() {
        return today;
    }

    /**
     * @param today the today to set
     */
    public void setToday(Date today) {
        this.today = today;
    }

    /**
     * @return the nuevaPregunta
     */
    public ComentarioDTO getNuevaPregunta() {
        return nuevaPregunta;
    }

    /**
     * @param nuevaPregunta the nuevaPregunta to set
     */
    public void setNuevaPregunta(ComentarioDTO nuevaPregunta) {
        this.nuevaPregunta = nuevaPregunta;
    }

    /**
     * @return the cantidadProductos
     */
    public int getCantidadProductos() {
        return cantidadProductos;
    }

    /**
     * @param cantidadProductos the cantidadProductos to set
     */
    public void setCantidadProductos(int cantidadProductos) {
        this.cantidadProductos = cantidadProductos;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the userRating
     */
    public double getUserRating() {
        return userRating;
    }

    /**
     * @param userRating the userRating to set
     */
    public void setUserRating(double userRating) {
        this.userRating = userRating;
    }

    /**
     * @return the horaInicioAlquiler
     */
    public Date getHoraInicioAlquiler() {
        return horaInicioAlquiler;
    }

    /**
     * @param horaInicioAlquiler the horaInicioAlquiler to set
     */
    public void setHoraInicioAlquiler(Date horaInicioAlquiler) {
        this.horaInicioAlquiler = horaInicioAlquiler;
    }

    /**
     * @return the denunciaId
     */
    public int getDenunciaId() {
        return denunciaId;
    }

    /**
     * @param denunciaId the denunciaId to set
     */
    public void setDenunciaId(int comentarioDenunciadoId) {
        this.denunciaId = comentarioDenunciadoId;
    }

    /**
     * @return the motivoDenuncia
     */
    public int getMotivoDenuncia() {
        return motivoDenuncia;
    }

    /**
     * @param motivoDenuncia the motivoDenuncia to set
     */
    public void setMotivoDenuncia(int motivoDenuncia) {
        this.motivoDenuncia = motivoDenuncia;
    }

    /**
     * @return the gMap
     */
    public MapModel getMapModel() {
        return mapModel;
    }

    /**
     * @param gMap the gMap to set
     */
    public void setMapModel(MapModel gMap) {
        this.mapModel = gMap;
    }

    public List<Integer> getIdImagenes() {
        return idImagenes;
    }

    public void setIdImagenes(List<Integer> idImagenes) {
        this.idImagenes = idImagenes;
    }

    public Integer getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(Integer publicacionId) {
        this.publicacionId = publicacionId;
    }

    public FavoritoMBean getFavorito() {
        return favorito;
    }

    public void setFavorito(FavoritoMBean favorito) {
        this.favorito = favorito;
    }

    public boolean isAddedToFavorito() {
        return addedToFavorito;
    }

    public void setAddedToFavorito(boolean addedToFavorito) {
        this.addedToFavorito = addedToFavorito;
    }

    /**
     * @return the ownerLogged
     */
    public boolean isOwnerLogged() {
        return ownerLogged;
    }
}
