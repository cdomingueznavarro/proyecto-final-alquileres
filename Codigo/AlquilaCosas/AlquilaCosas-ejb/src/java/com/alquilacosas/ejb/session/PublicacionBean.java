/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.ejb.session;

import com.alquilacosas.common.AlquilaCosasException;
import com.alquilacosas.common.NotificacionEmail;
import com.alquilacosas.dto.*;
import com.alquilacosas.ejb.entity.EstadoPublicacion.NombreEstadoPublicacion;
import com.alquilacosas.ejb.entity.Periodo.NombrePeriodo;
import com.alquilacosas.ejb.entity.*;
import com.alquilacosas.facade.*;
import java.util.*;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.jms.*;
import javax.persistence.NoResultException;
import org.apache.log4j.Logger;

/**
 *
 * @author ignaciogiagante
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PublicacionBean implements PublicacionBeanLocal {

    @Resource(name = "emailConnectionFactory")
    private ConnectionFactory connectionFactory;
    @Resource(name = "jms/notificacionEmailQueue")
    private Destination destination;
    @Resource
    private SessionContext context;
    @EJB
    private PeriodoAlquilerBeanLocal periodoBean;
    @EJB
    private PrecioBeanLocal precioBean;
    @EJB
    private UsuarioFacade usuarioFacade;
    @EJB
    private CategoriaFacade categoriaFacade;
    @EJB
    private PublicacionXEstadoFacade publicacionXEstadoFacade;
    @EJB
    private EstadoPublicacionFacade estadoPublicacionFacade;
    @EJB
    private PublicacionFacade publicacionFacade;
    @EJB
    private ImagenPublicacionFacade imagenPublicacionFacade;
    @EJB
    private PrecioFacade precioFacade;
    @EJB
    private PeriodoFacade periodoFacade;
    @EJB
    private AlquilerFacade alquilerFacade;
    @EJB
    private AlquilerXEstadoFacade estadoAlquiler;
    @EJB
    private CalificacionFacade calificacionFacade;
    @EJB
    private ComentarioFacade comentarioFacade;

    @Override
    public Integer registrarPublicacion(String titulo, String descripcion,
            Date fechaDesde, Date fechaHasta, boolean destacada, int cantidad,
            int usuarioId, int categoria, Double precioHora, Double precioDia,
            Double precioSemana, Double precioMes, 
            List<byte[]> imagenes, int periodoMinimo, int periodoMinimoFk,
            Integer periodoMaximo, Integer periodoMaximoFk, double latitud,
            double longitud) throws AlquilaCosasException {

        Publicacion publicacion = new Publicacion();
        publicacion.setTitulo(titulo);
        publicacion.setDescripcion(descripcion);
        publicacion.setFechaDesde(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, 2);
        publicacion.setFechaHasta(calendar.getTime());
        publicacion.setDestacada(destacada);
        publicacion.setCantidad(cantidad);

        Usuario usuario = null;

        try {
            usuario = usuarioFacade.find(usuarioId);
        } catch (NoResultException e) {
            context.setRollbackOnly();
            throw new AlquilaCosasException("No se encontro el Usuario en la "
                    + "base de datos.");
        }
        // Registrar categoria
        try {
            Categoria c = categoriaFacade.find(categoria);
            publicacion.setCategoriaFk(c);
        } catch (NoResultException e) {
            context.setRollbackOnly();
            throw new AlquilaCosasException("No se encontro la Categoria en la "
                    + "base de datos.");
        }

        // Fijar estado de publicacion
        EstadoPublicacion estadoPublicacion = null;
        try {
            estadoPublicacion = estadoPublicacionFacade.findByNombre(NombreEstadoPublicacion.ACTIVA);
        } catch (NoResultException e) {
            context.setRollbackOnly();
            throw new AlquilaCosasException("No se encontro el estado de la publicacion"
                    + " en la base de datos.");
        }
        PublicacionXEstado pxe = new PublicacionXEstado(publicacion, estadoPublicacion);
        publicacion.agregarPublicacionXEstado(pxe);

        // registrar periodos de alquiler
        Periodo periodo1 = periodoFacade.find(periodoMinimoFk);
        publicacion.setMinPeriodoAlquilerFk(periodo1);
        publicacion.setMinValor(periodoMinimo);

        int maxCantidadDias = -1;
        if (periodoMaximoFk != null && periodoMaximoFk > 0) {
            Periodo periodo2 = periodoFacade.find(periodoMaximoFk);
            publicacion.setMaxPeriodoAlquilerFk(periodo2);
            publicacion.setMaxValor(periodoMaximo);
            if(periodo2.getNombre() == NombrePeriodo.HORA) {
                maxCantidadDias = periodoMaximo > 24 ? 1: 0;
            } else if(periodo2.getNombre() == NombrePeriodo.DIA) {
                maxCantidadDias = periodoMaximo;
            } else if(periodo2.getNombre() == NombrePeriodo.SEMANA) {
                maxCantidadDias = periodoMaximo * 7;
            } else if(periodo2.getNombre() == NombrePeriodo.MES) {
                maxCantidadDias = periodoMaximo * 30;
            }
        }
        
        // Registrar precios
        Precio precio = null;
        Periodo periodo = null;
        if(precioHora != null) {
            periodo = periodoBean.getPeriodo(NombrePeriodo.HORA);
            precio = new Precio(precioHora, periodo);
            publicacion.agregarPrecio(precio);
        }
        periodo = periodoBean.getPeriodo(NombrePeriodo.DIA);
        precio = new Precio(precioDia, periodo);
        publicacion.agregarPrecio(precio);
        if(maxCantidadDias >= 7) {
            if(precioSemana == null) {
                precioSemana = precioDia * 7;
            }
            periodo = periodoBean.getPeriodo(NombrePeriodo.SEMANA);
            precio = new Precio(precioSemana, periodo);
            publicacion.agregarPrecio(precio);
        }
        if(maxCantidadDias >= 30) {
            if(precioMes == null) {
                precioMes = precioSemana * 4;
            }
            periodo = periodoBean.getPeriodo(NombrePeriodo.MES);
            precio = new Precio(precioMes, periodo);
            publicacion.agregarPrecio(precio);
        }        
        
        // Agregar imagenes
        for (byte[] bytes : imagenes) {
            ImagenPublicacion ip = new ImagenPublicacion();
            ip.setImagen(bytes);
            publicacion.agregarImagen(ip);
        }

        usuario.agregarPublicacion(publicacion);
        publicacion = publicacionFacade.create(publicacion);
        return publicacion.getPublicacionId();
    }

    @Override
    @PermitAll
    public PublicacionDTO getDatosPublicacion(int publicacionId) throws AlquilaCosasException {

        Publicacion p = publicacionFacade.find(publicacionId);
        PublicacionXEstado pxe = publicacionXEstadoFacade.getPublicacionXEstado(p);

        if (pxe == null) {
            throw new AlquilaCosasException("PublicacionXEstado no encontrado para la publicacion seleccionada.");
        }
        Periodo periodo1 = null;
        Periodo periodo2 = null;
        try {
            periodo1 = periodoFacade.getPeriodo(p.getMinPeriodoAlquilerFk().getPeriodoId());
            if(p.getMaxPeriodoAlquilerFk() != null && p.getMaxPeriodoAlquilerFk().getPeriodoId() != null) {
                periodo2 = periodoFacade.getPeriodo(p.getMaxPeriodoAlquilerFk().getPeriodoId());
            }
        } catch (Exception e) {
            Logger.getLogger(PublicacionBean.class).error("getDatosPublicacion("
                    + publicacionId + "). El periodo es nulo.");
        }

        PublicacionDTO publicacionDto = new PublicacionDTO(
                p.getPublicacionId(), p.getTitulo(), p.getDescripcion(),
                p.getFechaDesde(), p.getFechaHasta(), p.getDestacada(),
                p.getCantidad(), p.getCategoriaFk(), p.getImagenPublicacionList(),
                pxe.getEstadoPublicacion().getNombre(), p.getMinValor(), periodo1,
                p.getMaxValor(), periodo2);

        for (Precio precio : precioFacade.buscarActualesPorPublicacion(p)) {
            if (precio != null) {
                publicacionDto.getPrecios().add(new PrecioDTO(precio.getPrecioId(),
                        precio.getPeriodoFk().getPeriodoId(),
                        precio.getPrecio(),
                        precio.getPeriodoFk().getNombre()));
            }
        }
        return publicacionDto;
    }

    @Override
    public void actualizarPublicacion(int publicacionId, String titulo, String descripcion,
            Date fechaDesde, Date fechaHasta, boolean destacada, int cantidad,
            int usuarioId, int categoria, Double precioHora, Double precioDia,
            Double precioSemana, Double precioMes, 
            List<byte[]> imagenesAgregar, List<Integer> imagenesABorrar,
            int periodoMinimo, int periodoMinimoFk, Integer periodoMaximo,
            Integer periodoMaximoFk, NombreEstadoPublicacion estadoPublicacion) throws AlquilaCosasException {

        Publicacion publicacion = null;
        try {
            publicacion = publicacionFacade.find(publicacionId);
        } catch (NoResultException e) {
            throw new AlquilaCosasException("No se encontro la Publicacion en la "
                    + "base de datos.");
        }
        publicacion.setTitulo(titulo);
        publicacion.setDescripcion(descripcion);
        publicacion.setDestacada(destacada);
        publicacion.setCantidad(cantidad);

        // Actualizar categoria
        try {
            Categoria c = categoriaFacade.find(categoria);
            publicacion.setCategoriaFk(c);
        } catch (NoResultException e) {
            throw new AlquilaCosasException("No se encontro la Categoria en la "
                    + "base de datos." + e.getMessage());
        }

        // Actualizar estado
        PublicacionXEstado pxe = publicacionXEstadoFacade.getPublicacionXEstado(publicacion);
        if (pxe.getEstadoPublicacion().getNombre() != estadoPublicacion) {

            Calendar hoy = Calendar.getInstance();
            hoy.add(Calendar.DATE, 60);
            pxe.setFechaHasta(hoy.getTime());
            EstadoPublicacion ep = null;

            
            ep = estadoPublicacionFacade.findByNombre(estadoPublicacion);
            if(ep == null) {
                throw new AlquilaCosasException("No se encontro el Estado en la base de datos.");
            }

            PublicacionXEstado pxeNuevo = new PublicacionXEstado(publicacion, ep);
            publicacion.agregarPublicacionXEstado(pxeNuevo);
        }

        // Actualizar periodos minimos y maximos de alquiler
        Periodo periodo1 = periodoFacade.find(periodoMinimoFk);
        publicacion.setMinPeriodoAlquilerFk(periodo1);
        publicacion.setMinValor(periodoMinimo);
        int maxCantidadDias = Integer.MAX_VALUE;
        if (periodoMaximoFk != null && periodoMaximoFk > 0 && periodoMaximo != null && periodoMaximo > 0 ) {
            Periodo periodo2 = periodoFacade.find(periodoMaximoFk);
            publicacion.setMaxPeriodoAlquilerFk(periodo2);
            publicacion.setMaxValor(periodoMaximo);
            if(periodo2.getNombre() == NombrePeriodo.HORA) {
                maxCantidadDias = periodoMaximo > 24 ? 1: 0;
            } else if(periodo2.getNombre() == NombrePeriodo.DIA) {
                maxCantidadDias = periodoMaximo;
            } else if(periodo2.getNombre() == NombrePeriodo.SEMANA) {
                maxCantidadDias = periodoMaximo * 7;
            } else if(periodo2.getNombre() == NombrePeriodo.MES) {
                maxCantidadDias = periodoMaximo * 31;
            }
        }
        
        if(maxCantidadDias <= 7) {
            precioSemana = null;
            precioMes = null;
        } else {
            if(precioSemana == null)
                precioSemana = precioDia * 7;
            if(maxCantidadDias <= 31) {
                precioMes = null;
            } else if(precioMes == null) {
                    precioMes = precioSemana * 4;
            }
        }
        
        Precio precio = null;
        Periodo periodo = null;
        List<Precio> precios = precioFacade.buscarActualesPorPublicacion(publicacion);
        List<Precio> agregar = new ArrayList<Precio>();
        for(Precio p: precios) {
            if(p.getPeriodoFk().getNombre() == NombrePeriodo.HORA) {
                if(precioHora == null) {
                    p.setFechaHasta(new Date());
                }
                else if(p.getPrecio() != precioHora) {
                    p.setFechaHasta(new Date());
                    periodo = periodoFacade.findByNombre(NombrePeriodo.HORA);
                    precio = new Precio(precioHora, periodo);
                    agregar.add(precio);
                }
                precioHora = -1D;
            } else if(p.getPeriodoFk().getNombre() == NombrePeriodo.DIA) {
                if(p.getPrecio() != precioDia) {
                    p.setFechaHasta(new Date());
                    periodo = periodoFacade.findByNombre(NombrePeriodo.DIA);
                    precio = new Precio(precioDia, periodo);
                    agregar.add(precio);
                }
                precioDia = -1D;
            }else if(p.getPeriodoFk().getNombre() == NombrePeriodo.SEMANA) {
                if(precioSemana == null) {
                    p.setFechaHasta(new Date());
                }
                else if(p.getPrecio() != precioSemana) {
                    p.setFechaHasta(new Date());
                    periodo = periodoFacade.findByNombre(NombrePeriodo.SEMANA);
                    precio = new Precio(precioSemana, periodo);
                    agregar.add(precio);
                }
                precioSemana = -1D;
            } else if(p.getPeriodoFk().getNombre() == NombrePeriodo.MES) {
                if(precioMes == null) {
                    p.setFechaHasta(new Date());
                }
                else if(p.getPrecio() != precioMes) {
                    p.setFechaHasta(new Date());
                    periodo = periodoFacade.findByNombre(NombrePeriodo.MES);
                    precio = new Precio(precioMes, periodo);
                    agregar.add(precio);
                }
                precioMes = -1D;
            }
        }
        for(Precio p: agregar) {
            publicacion.agregarPrecio(p);
        }
        if(precioHora != null && precioHora != -1) {
            periodo = periodoFacade.findByNombre(NombrePeriodo.HORA);
            precio = new Precio(precioHora, periodo);
            publicacion.agregarPrecio(precio);
        }
        if(precioSemana != null && precioSemana != -1) {
            periodo = periodoFacade.findByNombre(NombrePeriodo.SEMANA);
            precio = new Precio(precioSemana, periodo);
            publicacion.agregarPrecio(precio);
        }
        if(precioMes != null && precioMes != -1) {
            periodo = periodoFacade.findByNombre(NombrePeriodo.MES);
            precio = new Precio(precioMes, periodo);
            publicacion.agregarPrecio(precio);
        }
        

        // Borrar imagenes removidas
        if (imagenesABorrar != null) {
            for (Integer i : imagenesABorrar) {
                ImagenPublicacion ip = imagenPublicacionFacade.find(i);
                publicacion.removerImagen(ip);
            }
        }
        // agregar imagenes nuevas
        for (byte[] imagen : imagenesAgregar) {
            ImagenPublicacion ip = new ImagenPublicacion();
            ip.setImagen(imagen);
            imagenPublicacionFacade.create(ip);
            publicacion.agregarImagen(ip);
        }

        publicacion = publicacionFacade.edit(publicacion);
    }

    @Override
    @PermitAll
    public PublicacionDTO getPublicacion(int id) {
        Publicacion publicacion = publicacionFacade.find(id);
        PublicacionDTO resultado = null;
        if (publicacion != null) {
            resultado = new PublicacionDTO(publicacion.getPublicacionId(), publicacion.getTitulo(),
                    publicacion.getDescripcion(), publicacion.getFechaDesde(), publicacion.getFechaHasta(),
                    publicacion.getDestacada(), publicacion.getCantidad());

            resultado.setPeriodoMinimoValor(publicacion.getMinValor());
            resultado.setPeriodoMinimo(publicacion.getMinPeriodoAlquilerFk());
            if (publicacion.getMaxPeriodoAlquilerFk() != null) {
                resultado.setPeriodoMaximoValor(publicacion.getMaxValor());
                resultado.setPeriodoMaximo(publicacion.getMaxPeriodoAlquilerFk());
            } else {
                resultado.setPeriodoMaximoValor(100);
                Periodo temp = new Periodo();
                temp.setNombre(NombrePeriodo.DIA);
                resultado.setPeriodoMaximo(temp);
            }

            UsuarioDTO propietario = new UsuarioDTO(publicacion.getUsuarioFk());
            propietario.setUsername(publicacion.getUsuarioFk().getLoginList().get(0).getUsername());
            resultado.setPropietario(propietario);

            Domicilio domicilio = publicacion.getUsuarioFk().getDomicilioList().get(0);
            resultado.setProvincia(domicilio.getProvinciaFk().getNombre());
            resultado.setCiudad(domicilio.getProvinciaFk().getNombre());
            resultado.setBarrio(domicilio.getBarrio());
            resultado.setLatitud(domicilio.getLatitud());
            resultado.setLongitud(domicilio.getLongitud());
            resultado.setImagenIds(getIdImagenes(publicacion));

            List<PrecioDTO> precios = precioBean.getPrecios(publicacion);
            resultado.setPrecios(precios);
            resultado.setCategoriaF(new CategoriaDTO(publicacion.getCategoriaFk().getCategoriaId(),
                    publicacion.getCategoriaFk().getNombre()));
        }
        return resultado;
    }

    private List<Integer> getIdImagenes(Publicacion publicacion) {
        List<Integer> imagenes = new ArrayList<Integer>();
        for (ImagenPublicacion imagen : publicacion.getImagenPublicacionList()) {
            imagenes.add(imagen.getImagenPublicacionId());
        }
        return imagenes;
    }

    @Override
    @PermitAll
    public List<ComentarioDTO> getPreguntas(int publicationId) {
        Publicacion filter = publicacionFacade.find(publicationId);
        List<Comentario> comentarios = comentarioFacade.findPreguntasByPublicacion(filter);
        List<ComentarioDTO> resultado = new ArrayList<ComentarioDTO>();
        Comentario respuestaTemp = null;
        ComentarioDTO respuesta = null;
        for (Comentario comentario : comentarios) {
            respuesta = null;
            respuestaTemp = comentario.getRespuesta();
            if (respuestaTemp != null) {
                respuesta = new ComentarioDTO(respuestaTemp.getComentarioId(),
                        respuestaTemp.getComentario(), respuestaTemp.getFecha(),
                        respuestaTemp.getUsuarioFk().getUsuarioId(),
                        respuestaTemp.getUsuarioFk().getNombre(), null);
                respuesta.setBaneado(respuestaTemp.getBaneado());
            }
            ComentarioDTO pregunta = new ComentarioDTO(comentario.getComentarioId(),
                    comentario.getComentario(), comentario.getFecha(),
                    comentario.getUsuarioFk().getUsuarioId(),
                    comentario.getUsuarioFk().getNombre(), respuesta);
            pregunta.setBaneado(comentario.getBaneado());

            resultado.add(pregunta);
        }
        return resultado;
    }

    @Override
    public void setPregunta(int publicacionId, ComentarioDTO nuevaPregunta)
            throws AlquilaCosasException {
        Comentario pregunta = new Comentario();
        Publicacion publicacion = publicacionFacade.find(publicacionId);
        pregunta.setComentario(nuevaPregunta.getComentario());
        pregunta.setFecha(nuevaPregunta.getFecha());
        pregunta.setPregunta(Boolean.TRUE);
        Usuario usuario = usuarioFacade.find(nuevaPregunta.getUsuarioId());
        pregunta.setUsuarioFk(usuario);
        pregunta.setPublicacionFk(publicacion);
        pregunta.setBaneado(Boolean.FALSE);
        comentarioFacade.create(pregunta);

        // Enviar email de notificacion
        Usuario usuarioDueno = publicacion.getUsuarioFk();
        try {
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(true,
                    Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);
            ObjectMessage message = session.createObjectMessage();

            String asunto = "Has recibido una pregunta por tu articulo " + publicacion.getTitulo();
            String texto = "<html>Hola " + usuarioDueno.getNombre() + ", <br/><br/>"
                    + "Has recibido una pregunta por tu articulo <b>" + publicacion.getTitulo() + "</b>: <br/><br/>"
                    + "'" + nuevaPregunta.getComentario() + "' <br/><br/>"
                    + "Para responder esta pregunta ingresa a tu panel de usuario. <br/><br/>"
                    + "Atentamente, <br/> <b>AlquilaCosas </b>";
            NotificacionEmail notificacion = new NotificacionEmail(usuarioDueno.getEmail(), asunto, texto);
            message.setObject(notificacion);
            producer.send(message);
            session.close();
            connection.close();

        } catch (Exception e) {
            throw new AlquilaCosasException("Excepcion al enviar la notificacion" + e.getMessage());
        }
    }

    @Override
    public List<ComentarioDTO> getPreguntasSinResponder(int usuarioId) {
        Usuario filter = usuarioFacade.find(usuarioId);
        List<Comentario> comentarios = comentarioFacade.findPreguntasSinResponderByUsuario(filter);
        List<ComentarioDTO> resultado = new ArrayList<ComentarioDTO>();
        Comentario respuesta = null;
        for (Comentario comentario : comentarios) {
            ComentarioDTO tempComentario = new ComentarioDTO(comentario.getComentarioId(),
                    comentario.getComentario(), comentario.getFecha(),
                    comentario.getUsuarioFk().getUsuarioId(),
                    comentario.getUsuarioFk().getNombre(), comentario.getPublicacionFk().getPublicacionId(), null);
            tempComentario.setPublicationTitle(comentario.getPublicacionFk().getTitulo());
            tempComentario.setImageId(
                    comentario.getPublicacionFk().getImagenPublicacionList().isEmpty()?
                    (-1):comentario.getPublicacionFk().getImagenPublicacionList().get(0).getImagenPublicacionId());
            resultado.add(tempComentario);
            
        }

        return resultado;
    }

    @Override
    public void setRespuesta(ComentarioDTO preguntaConRespuesta)
            throws AlquilaCosasException {

        Comentario pregunta = comentarioFacade.find(preguntaConRespuesta.getId());
        Publicacion publicacion = pregunta.getPublicacionFk();

        Comentario respuesta = new Comentario();
        respuesta.setComentario(preguntaConRespuesta.getRespuesta().getComentario());
        respuesta.setFecha(preguntaConRespuesta.getRespuesta().getFecha());
        respuesta.setPregunta(Boolean.FALSE);

        Usuario usuarioResponde = usuarioFacade.find(preguntaConRespuesta.getRespuesta().getUsuarioId());
        respuesta.setUsuarioFk(usuarioResponde);
        respuesta.setPublicacionFk(publicacion);
        respuesta.setBaneado(Boolean.FALSE);
        pregunta.setRespuesta(respuesta);
        comentarioFacade.create(respuesta);

        Usuario usuarioPregunto = pregunta.getUsuarioFk();

        try {
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(true,
                    Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);
            ObjectMessage message = session.createObjectMessage();

            String asunto = "Han respondido tu pregunta por el articulo " + publicacion.getTitulo();
            String texto = "<html>Hola " + usuarioPregunto.getNombre() + ", <br/><br/>"
                    + "Han respondido tu pregunta por el articulo <b>" + publicacion.getTitulo() + "</b>: <br/><br/>"
                    + "'" + respuesta.getComentario() + "' <br/>"
                    + "<br/><br/>"
                    + "Atentamente, <br/> <b>AlquilaCosas </b>";
            NotificacionEmail notificacion = new NotificacionEmail(usuarioPregunto.getEmail(), asunto, texto);
            message.setObject(notificacion);
            producer.send(message);
            session.close();
            connection.close();

        } catch (Exception e) {
            throw new AlquilaCosasException(e.getMessage());
        }

    }

    @Override
    @PermitAll
    public List<Periodo> getPeriodos() {
        return periodoFacade.getPeriodosOrderByHoras();
    }

    @Override
    @PermitAll
    public List<Date> getFechasSinStock(int publicationId, int cantidad)
            throws AlquilaCosasException {
        Publicacion publicacion = publicacionFacade.find(publicationId);
        if (publicacion == null) {
            throw new AlquilaCosasException("Publicación inexistente.");
        }
        List<Date> respuesta = new ArrayList<Date>();
        List<Alquiler> alquileres = alquilerFacade.getAlquileresByPublicacionFromToday(publicacion);
        Iterator<Alquiler> itAlquiler = alquileres.iterator();

        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        int disponibles = publicacion.getCantidad();

        HashMap<String, Integer> dataCounter = new HashMap(60);//probablemente no existan pedidos mas alla de 60 dias desde hoy
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(new Date());

        while (itAlquiler.hasNext()) {

            Alquiler temp = itAlquiler.next();
            Calendar date = Calendar.getInstance();
            date.setTime(temp.getFechaInicio());
            date.set(Calendar.HOUR_OF_DAY, 0);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            Calendar fechaFin = Calendar.getInstance();
            fechaFin.setTime(temp.getFechaFin());
            if (fechaFin.get(Calendar.HOUR_OF_DAY) == 0 && fechaFin.get(Calendar.MINUTE) == 0
                    && fechaFin.get(Calendar.SECOND) == 0) //fechaFin.add(Calendar.SECOND, 1); 
            //No me importan las fechas anteriores a hoy, no son seleccionables
            {
                if (date.before(today)) {
                    date.setTime(today.getTime());
                }
            }
            //Me fijo si en los dias que dura el alquiler analizado hay disponibilidad de
            //productos para el pedido que estoy creando
            if (cantidad <= disponibles - temp.getCantidad()) //Si alcanzan los dias guardo en el hashmap el dia y la cantidad de
            //productos del alquiler, para ir acumulando esta cantidad para cada fecha
            //al final me fijo por cada fecha si alcanza, porque ya tengo todos los 
            //alquileres analizados
            {
                while (date.before(fechaFin)) {
                    Integer acumulado = dataCounter.get(date.getTime().toString());
                    if (acumulado != null) {
                        dataCounter.put(date.getTime().toString(), new Integer(acumulado + temp.getCantidad()));
                    } else {
                        dataCounter.put(date.getTime().toString(), new Integer(temp.getCantidad()));
                    }
                    date.add(Calendar.DATE, 1);
                }
            } else //si no alcanza, marco a todos los dias de este alquiler en el hasmap con el valor
            //de la disponibilidad total de la publicacion, lo cual significa que no va a alcanzar
            //ese dia para hacer el pedido ni siquiera de 1 producto
            {
                while (date.before(fechaFin)) { //temp.getFechaFin())){
                    dataCounter.put(date.getTime().toString(), new Integer(disponibles));
                    date.add(Calendar.DATE, 1);
                }
            }

            if (date.after(lastDate)) {
                lastDate = date;
            }

        }
        //Reviso el hashmap y voy llenando la lista de respuesta con las fechas que 
        //no me alcanza el stock disponible
        Calendar date = Calendar.getInstance();
        date.setTime(new Date());
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);

        if (lastDate.get(Calendar.HOUR_OF_DAY) == 0 && lastDate.get(Calendar.MINUTE) == 0
                && lastDate.get(Calendar.SECOND) == 0) {
            lastDate.add(Calendar.SECOND, 1);
        }
        while (date.before(lastDate)) {
            Integer acumulado = dataCounter.get(date.getTime().toString());
            if (acumulado != null) {
                if (cantidad > (disponibles - acumulado)) {
                    respuesta.add(date.getTime());
                }
            }
            date.add(Calendar.DATE, 1);
        }

        return respuesta;
    }

    @Override
    public void crearPedidoAlquiler(int publicationId, int usuarioId,
            Date beginDate, Date endDate, double monto, int cantidad) throws AlquilaCosasException {
        Alquiler nuevoPedido = new Alquiler();
        Publicacion publicacion = publicacionFacade.find(publicationId);
        Usuario propietario = publicacion.getUsuarioFk();
        nuevoPedido.setPublicacionFk(publicacion);
        nuevoPedido.setUsuarioFk(usuarioFacade.find(usuarioId));
        nuevoPedido.setFechaInicio(beginDate);
        nuevoPedido.setFechaFin(endDate);
        nuevoPedido.setMonto(monto);
        nuevoPedido.setCantidad(cantidad);

        alquilerFacade.create(nuevoPedido);

        estadoAlquiler.saveState(nuevoPedido, EstadoAlquiler.NombreEstadoAlquiler.PEDIDO);

        try {
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(true,
                    Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);
            ObjectMessage message = session.createObjectMessage();

            String asunto = "Han solicitado alquilar el articulo " + publicacion.getTitulo();
            StringBuilder texto = new StringBuilder();
            texto.append("<html>Hola ");
            texto.append(propietario.getNombre());
            texto.append(", <br/><br/> Has recibido una solicitud de alquiler por el articulo <b>");
            texto.append(publicacion.getTitulo());
            texto.append("</b> <br/><br/>");
            texto.append(" Para aceptarlo, ingresa a tu panel de usuario en alquilaCosas.com.ar ");
            texto.append("<br/><br/><br/> Atentamente, <br/> <b>AlquilaCosas </b>");

            NotificacionEmail notificacion = new NotificacionEmail(propietario.getEmail(), asunto, texto.toString());
            message.setObject(notificacion);
            producer.send(message);
            session.close();
            connection.close();

        } catch (Exception e) {
            throw new AlquilaCosasException(e.getMessage());
        }
    }

    @Override
    @PermitAll
    public double getUserRate(UsuarioDTO propietario) {
        double rating = calificacionFacade.getCalificacionByUsuario(propietario.getId());
        rating *= 0.5;
        rating += 5;
        return rating;//hacer el metodo!!
    }

    private double redondearDecimal(double d, double c) {
        int temp = (int) ((d * Math.pow(10, c)));
        return (((double) temp) * Math.pow(10, -c));
    }
}
