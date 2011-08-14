/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alquilacosas.ejb.session;

import com.alquilacosas.common.AlquilaCosasException;
import com.alquilacosas.common.CategoriaFacade;
import com.alquilacosas.ejb.entity.Categoria;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author wilson
 */
@Stateless
public class CategoriaBean implements CategoriaBeanLocal {

     @PersistenceContext(unitName="AlquilaCosas-ejbPU") 
     private EntityManager entityManager;
     
     @Override
     public List<Categoria> getCategorias() {
          List<Categoria> categorias;
          Query query = entityManager.createNamedQuery("Categoria.findAll");
          categorias = query.getResultList();
          return categorias;
     }
     
     // Add business logic below. (Right-click in editor and choose
     // "Insert Code > Add Business Method")
     
     @Override
     public void registrarCategoria(String nombre, String descripcion, Categoria categoriaPadre) throws AlquilaCosasException{
          Categoria nuevaCategoria = new Categoria();
          nuevaCategoria.setNombre(nombre);
          nuevaCategoria.setDescripcion(descripcion);
          if(categoriaPadre != null)
               nuevaCategoria.setCategoriaFk(categoriaPadre);
          try{
               entityManager.persist(nuevaCategoria);
          }
          catch(Exception e){
               throw new AlquilaCosasException("Error al insertar la Categoria - " + e.getMessage());
          }
          entityManager.flush();
     }
     
     @Override
     public void borrarCategoria(Categoria categoria) {
          //no se como se hace...
          Categoria modifCategoria = entityManager.find(Categoria.class, categoria.getCategoriaId());
          entityManager.remove(modifCategoria);
     }

     @Override
     public void modificarCategoria(Categoria categoria) {
          Categoria modifCategoria = entityManager.find(Categoria.class, categoria.getCategoriaId());
          modifCategoria.setNombre(categoria.getNombre());
          modifCategoria.setDescripcion(categoria.getDescripcion());
          entityManager.merge(modifCategoria);
     }
     
    @Override
     public List<CategoriaFacade> getCategoriaFacade() {
         List<Categoria> categorias;
          Query query = entityManager.createNamedQuery("Categoria.findAll");
          categorias = query.getResultList();
          List<CategoriaFacade> catFacade = new ArrayList<CategoriaFacade>();
          for(Categoria c: categorias) {
              Categoria padre = c.getCategoriaFk();
              int padreId = 0;
              if(padre != null)
                  padreId = padre.getCategoriaId();
              CategoriaFacade cat = new CategoriaFacade(c.getCategoriaId(), 
                      padreId, c.getNombre(), c.getDescripcion());
              catFacade.add(cat);
          }
          return catFacade;
     }

    @Override
    public List<CategoriaFacade> getSubCategorias(int categoria) {
        
        Categoria cat = entityManager.find(Categoria.class, categoria);
        Query subcatQuery = entityManager.createNamedQuery("Categoria.findByCategoriaFk");
        subcatQuery.setParameter("categoria", cat);
        List<Categoria> categorias = subcatQuery.getResultList();
        List<CategoriaFacade> subcategorias = new ArrayList<CategoriaFacade>();
        for(Categoria c: categorias) {
            subcategorias.add(new CategoriaFacade(c.getCategoriaId(), c.getNombre()));
        }
        return subcategorias;        
    }
    
    

}