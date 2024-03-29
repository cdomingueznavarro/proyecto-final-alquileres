/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.mbean;

import com.alquilacosas.common.AlquilaCosasException;
import com.alquilacosas.dto.ComentarioDTO;
import com.alquilacosas.dto.PublicacionDTO;
import com.alquilacosas.ejb.session.PublicacionBeanLocal;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.apache.log4j.Logger;

/**
 *
 * @author jorge
 */
@ManagedBean (name = "desplieguePreguntas")
@ViewScoped
public class ResponderPreguntasMBean implements Serializable {

    @EJB
    private PublicacionBeanLocal publicationBean;
    @ManagedProperty (value="#{login}")
    private ManejadorUsuarioMBean login;
    private PublicacionDTO publicacion;
    private String effect;
    private List<ComentarioDTO> comentarios;
    private ComentarioDTO nuevaRespuesta;
    private ComentarioDTO selectedPregunta;
    private int cantidad;

    
    
    
    /** Creates a new instance of ResponderPreguntasMBean */
    public ResponderPreguntasMBean() { }
    
    @PostConstruct
    public void init(){
        Logger.getLogger(ResponderPreguntasMBean.class).debug("ResponderPreguntasMBean: postconstruct.");
        setEffect("fade"); 
            setNuevaRespuesta(new ComentarioDTO());
            comentarios = publicationBean.getPreguntasSinResponder(login.getUsuarioId());
            cantidad = comentarios.size();
        }

     public void guardarPregunta() {  
         
        nuevaRespuesta.setUsuarioId(getLogin().getUsuarioId());
        nuevaRespuesta.setUsuario(getLogin().getUsername());
        nuevaRespuesta.setFecha(new Date());
        selectedPregunta.setRespuesta(nuevaRespuesta);
         try {
             publicationBean.setRespuesta(selectedPregunta);
             setNuevaRespuesta(new ComentarioDTO());  
         } catch (AlquilaCosasException e) {
             FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error al enviar respuesta", e.getMessage()));
         }
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
     * @return the nuevaRespuesta
     */
    public ComentarioDTO getNuevaRespuesta() {
        return nuevaRespuesta;
    }

    /**
     * @param nuevaRespuesta the nuevaRespuesta to set
     */
    public void setNuevaRespuesta(ComentarioDTO nuevaRespuesta) {
        this.nuevaRespuesta = nuevaRespuesta;
    }

    /**
     * @return the selectedPregunta
     */
    public ComentarioDTO getSelectedPregunta() {
        return selectedPregunta;
    }

    /**
     * @param selectedPregunta the selectedPregunta to set
     */
    public void setSelectedPregunta(ComentarioDTO selectedPregunta) {
        this.selectedPregunta = selectedPregunta;
    }

    public ManejadorUsuarioMBean getLogin() {
        return login;
    }

    public void setLogin(ManejadorUsuarioMBean usuarioLogueado) {
        this.login = usuarioLogueado;
    }

    /**
     * @return the cantidad
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * @param cantidad the cantidad to set
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

}
