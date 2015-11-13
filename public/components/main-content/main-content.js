define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app','jquery.sticky'], function(bridget,ko, template,Isotope,imagesLoaded,app,sticky) {
	
	
	$.bridget('isotope', Isotope);
	
	
		
	ko.bindingHandlers.homeisotope = {
					init: app.initOrUpdate('init'),
					update: app.initOrUpdate('update')
				};
	
	
	// method to initialize filter stick plugin
	// dependency sticker
	// moved to here from plugin.js, no need to track scroll on the rest of the pages
	var initFilterStick = function(){

		// log
		//logger( 'info','plugins.js / initSticky' );
 
		if ( $( '.filter' ).length !== 0 ){

			// init sticky
			$( '.filter' ).sticky({
				topSpacing: 74
			});

			// on scroll
			$( window ).on( 'scroll touchmove', function(){

				// var
				if( $( "body" ).attr( 'data-page' )=== 'home' ) {
				var offset = $( '.partners' ).offset();
				if(offset){
					topPos = offset.top - 74;
	
					// set class
					if ( $( document ).scrollTop() >= topPos ){
	
						$( '.filter' ).addClass('unstick');
	
					} else {
	
						$( '.filter' ).removeClass('unstick');
	
					}
				}}
			});
		
	}};
	
					
	function FeaturedExhibit(data){
	  var fe=this;
	  fe.title=ko.observable();
	  fe.description=ko.observable();
	  fe.dbId=ko.observable(-1);
	  fe.thumbs=ko.observableArray();
	  fe.url=ko.observable("");
	 
	  fe.load=function(data){
		 fe.title(data.title);
	     fe.dbId(data.dbId);
	     if(data.isExhibition)
	      fe.url("#exhibitionview/"+data.dbId);
	     else{fe.url("#collectionview/"+data.dbId);}
	     fe.description(data.description);
		  var i=0;
		  var j=0;
		  
		  while (i<4 && j<data.firstEntries.length){
			  var thumburl="";
			  if(data.firstEntries[j].thumbnailUrl){
				  
				  if(data.firstEntries[j].thumbnailUrl){
						if (data.firstEntries[j].thumbnailUrl.indexOf('/') === 0) {
							thumburl=data.firstEntries[j].thumbnailUrl;
						} else {
							var newurl='url=' + encodeURIComponent(data.firstEntries[j].thumbnailUrl)+'&';
							thumburl='/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
						}}
					   else{
						   thumburl="img/content/thumb-empty.png";
					   }
				  
				  var thumb={url:thumburl,title:data.firstEntries[j].title};
				  fe.thumbs.push(thumb);
				  i++;}
			    j++
		  }}
	  if(data != undefined) fe.load(data);
	  
	}		
			
	 function Collection(data){
		 var self = this;


		  self.collname='';
		  self.id=-1;
		  self.url='';
		  self.owner='';
		  self.ownerId=-1;
		  self.itemCount=0;
		  self.thumbnail='../assets/img/content/thumb-empty.png';
		  self.description='';
		  self.isLoaded = ko.observable(false);
		  self.isExhibition=false;
		  self.itemcss="item ";
		  self.type="COLLECTION";
		  self.load=function(data){
			  if(data.title==undefined){
					self.collname="No title";
				}else{self.collname=data.title;}
				self.id=data.dbId;
				
				self.url="#collectionview/"+self.id;
				
				self.description=data.description;
				if(data.firstEntries.length>0){
					self.thumbnail=data.firstEntries[0].thumbnailUrl;
				}
				self.isExhibition=data.isExhibition;
				if(self.isExhibition){
					self.itemcss+="exhibition";
					self.type="EXHIBITION";
				}
				else{self.itemcss+="collection";}
				if(data.owner!==undefined){
						self.owner=data.owner;
					}

			  
		  }
		  self.cachedThumbnail = ko.pureComputed(function() {
				
			   if(self.thumbnail){
				if (self.thumbnail.indexOf('/') === 0) {
					return self.thumbnail;
				} else {
					var newurl='url=' + encodeURIComponent(self.thumbnail)+'&';
					return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
				}}
			   else{
				   return "img/content/thumb-empty.png";
			   }
			});
		  if(data != undefined) self.load(data);
		   
		  
	}
	
  function MainContentModel(params) {
	  this.route = params.route;
	  var self = this;
	  
	  $("div[role='main']").toggleClass( "homepage", true );
	  self.loading = ko.observable(false);
	  self.exhibitloaded=ko.observable(false);
	  self.featuredExhibition=ko.observable(null);	
	  self.featuredCollection=ko.observable(null);
	  self.homecollections=ko.observableArray();
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	 	
	  self.revealItems = function (data) {
		  if(data.length==0){ self.loading(false);$(".loadmore").text("no more results");}
			
			for (var i in data) {
				var c=new Collection(
						data[i]
						);
				self.homecollections().push(c);
			}
			self.homecollections.valueHasMutated();
		};
		
	  
	  self.loadAll = function () {
		  //this should replaced with get space collections + exhibitions
		   self.loading(true);
		 
		  var promiseCollections = self.getSpaceCollections();
		  $.when(promiseCollections).done(function(responseCollections) {
			        self.totalCollections(responseCollections.totalCollections);
			        self.totalExhibitions(responseCollections.totalExhibitions);
				    self.revealItems(responseCollections['collectionsOrExhibitions']);
				    initFilterStick();
					
			});
		  var promise2 = self.getFeatured("5624a338569e4959735d8558");
          $.when(promise2).done(function (data) {
        	  
        	 
        	  self.featuredExhibition(new FeaturedExhibit(data));
        	  $("div.featured-box.exhibition").find("div.featured-hero > img").attr("src",self.featuredExhibition().thumbs()[0].url);    
        	  self.exhibitloaded(true);
        	  
          });
          var promise3 = self.getFeatured("55b74e5b569e1b44eeac72c0");
          $.when(promise3).done(function (data) {
        	  
        	 
        	  self.featuredCollection(new FeaturedExhibit(data));
        	  $("div.featured-box.exhibition").find("div.featured-hero > img").attr("src",self.featuredCollection().thumbs()[0].url);    
        	
        	  WITHApp.initCharacterLimiter();
          });
		  
		};
		//TODO:get project from backend. Update hard-coded group name parameter in list collections call.
		self.getSpaceCollections = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "offset=0&count=20&collectionHits=true&isPublic=true",
			}).success (function(){
			});
		};
		
         self.getFeatured=function(id) {
			
			/*call must change to get featured exhibition for space*/
	        return $.ajax({
	            type: "GET",
	            url: "/collection/"+id,
	            success: function () {

	            }
	        });
	    };
		
      
		
	    
		self.loadNext = function () {
			self.moreCollections();
		};

		self.moreCollections = function () {
			if (self.loading === true) {
				setTimeout(self.moreCollections(), 1000);
			}
			if (self.loading() === false) {
				self.loading(true);
				var offset = self.homecollections().length+1;
				$.ajax({
					"url": "/collection/list?isPublic=true&count=20&offset=" + offset,
					"method": "get",
					"contentType": "application/json",
					"success": function (data) {
						self.revealItems(data['collectionsOrExhibitions']);
						
					},
					"error": function (result) {
						self.loading(false);
					}
				});
			}
		};

	  self.loadCollectionOrExhibition = function(item) {
		  if (item.isExhibition) {
			  window.location = 'index.html#exhibitionview/'+ item.id;
			  
		  }
		  else {
			  window.location = 'index.html#collectionview/' + item.id;
		  }
		  return false;
	  };
		
      self.loadAll();	  

		
	  
	  self.filter=function(data, event) {
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}
					  
	 

	  
	
  }
  
 
 
  return { viewModel: MainContentModel, template: template };
});