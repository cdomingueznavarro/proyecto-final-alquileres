/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function handleModificarCategoria(xhr, status, args) {
    if(args.validationFailed) {
        jQuery('#dialogo').effect("shake", {
            times:3
        }, 100);
    } else {
        dialogoModifCat.hide();
    }
}

function handleNuevoPeriodo(xhr, status, args) {
    if(args.validationFailed) {
        jQuery('#dialogo').effect("shake", {
            times:3
        }, 100);
    } else {
        dialogoNuevo.hide();
    }
}

function agregarPregunta(xhr, status, args) {
    if(args.logueado) {
        nuevaPregunta.show();
    }
    else {
        login.show();
    }
}

function revisarLogueo(xhr, status, args) {
    if(args.validationFailed || !args.logueado) {
        jQuery('#loginDialog').parent().effect("shake", {
            times:2
        }, 100);
    } else {
        loginDlg.hide();
        if(args.preguntar){
            loginDlg.hide();
            nuevaPregunta.show();
        }
        else if(args.alquilar){
            loginDlg.hide();
            confirmRent.show();
        }
    }
}

function revisarLogueo2(xhr, status, args) {
    if(args.logueado) {
       var url = window.location;
       window.location = url;
    }
}

function borrarImagen(img) {
    document.getElementById(':formPublicacion:' + img).style.display='none';
}

function highlightCalendar(specialDays, date, cssClass) {
    var d = date.getDate();
    var m = date.getMonth() + 1;
    var y = date.getFullYear();
    var result = [ true, null, '' ]; 

    if (specialDays[y] && specialDays[y][m]
        && specialDays[y][m][d]) {
        var s = specialDays[y][m][d];
        result = [ true, cssClass, '' ];
    }
    return result; // no change
}

function revisarDisponibilidad(xhr, status, args){
    if(!args.ownerLogged){
        if(args.hayDisponibilidad) {
            if(args.logueado)
                confirmRent.show();
            else
                login.show();
        }
    }
}


function revisarPeriodoSeleccionado(xhr, status, args){
    if(args.mostrarHoraInicio)
        jQuery('#horaInicio').disabled = false;
    else
        jQuery('#horaInicio').disabled = true;
}

function handleModificarAlquiler(xhr, status, args) {
    if(!args.validationFailed && args.modificado) {
        modificarDlg.hide();
    }
}

function handlePedirCambioAlquiler(xhr, status, args) {
    if(!args.validationFailed && args.enviado) {
        pedirCambioDlg.hide();
    }
}
    
function getUrlVars() {
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
    }
    return vars;
}

var currentMarker = null;

function handlePointClick(event) {  
        
    if(currentMarker == null) {  
        document.getElementById('lat').value = event.latLng.lat();  
        document.getElementById('lng').value = event.latLng.lng();  
  
        currentMarker = new google.maps.Marker({  
            position:new google.maps.LatLng(event.latLng.lat(), event.latLng.lng())  
        }); 
                              
        myMap.addOverlay(currentMarker);  
    }
    dlg.show();  
}  
  
function markerAddComplete() {  
    var title = "alquilacosas";//document.getElementById('title');  
    currentMarker.setTitle(title.value);  
    var geocoder = new google.maps.Geocoder();
    var lat = parseFloat(document.getElementById('lat').value);
    var lng = parseFloat(document.getElementById('lng').value);
    var latlng = new google.maps.LatLng(lat, lng);
    geocoder.geocode({'latLng': latlng}, function(results, status) {
      if (status == google.maps.GeocoderStatus.OK) {
          //document.getElementById('registro:barrio').value = results[0].formatted_address;
          var direction = results[0].address_components;
          //document.getElementById('registro:piso').value = direction[2].long_name;
          /*for(var i = 0; i< direction.length; ++i)
            document.getElementById('registro:' + direction[i].types[0]).value = direction[i].long_name;*/
            
            document.getElementById('registro:' + direction[0].types[0]).value = direction[0].long_name;   
            document.getElementById('registro:' + direction[1].types[0]).value = direction[1].long_name;   
            document.getElementById('registro:' + direction[2].types[0]).value = direction[2].long_name;   
            document.getElementById('registro:' + direction[3].types[0]).value = direction[3].long_name;   
            /*document.getElementById('registro:' + direction[4].types[0]).value = direction[4].long_name;   
            document.getElementById('registro:' + direction[5].types[0]).value = direction[5].long_name;   
            document.getElementById('registro:' + direction[6].types[0]).value = direction[6].long_name;*/
              

         
          
      } else {
        document.getElementById('direccion').value = "No ok!!";
      }
    });

    dlg.hide();   
    
}  
  
function cancel() {  
    dlg.hide();  
    currentMarker.setMap(null);  
    currentMarker = null;  
  
    return false;  
}  

function onMarkerDrag(event){
    currentMarker = event.getMarker();
    document.getElementById('lat').value = event.latLng.lat();  
    document.getElementById('lng').value = event.latLng.lng();          
}
    
function updateCoordinates(event){
    document.getElementById('lat').value = event.latLng.lat();  
    document.getElementById('lng').value = event.latLng.lng();          
}

function manejarPopup(comp, popupClass) {
    var detalle = comp[0];
    if(detalle.style.display == 'none') {
        $('.' + popupClass).css('display', 'none');
        detalle.style.display = 'block';
        
        $('.' + popupClass).bind("mouseleave",function() {
            $('.' + popupClass).hide();
        });
        
    } else {
        $('.' + popupClass).css('display', 'none');
    }
}
