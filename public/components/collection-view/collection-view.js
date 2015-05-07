define(['bridget','knockout', 'text!./collection-view.html','masonry','imagesloaded'], function(bridget,ko, template, masonry,imagesLoaded) {

	 $.bridget( 'masonry', masonry );	
	 
	 
	 
	 ko.bindingHandlers.masonry = { init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	var $element = $(element);
	    	    $element.masonry( {itemSelector: '.masonryitem',gutter: 10,isInitLayout: false});
			
			    ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
			       
			        $element.masonry("destroy");
			    });

	    },
	    update: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
	    	
	    	var $element = $(element),
	    	list = ko.utils.unwrapObservable(allBindingsAccessor().foreach)
	    	masonry = ko.utils.unwrapObservable(valueAccessor())
	    	if (!list.length){
				
				return;
			}
	    	
	        
	    	imagesLoaded( $element, function() {
	    		if (!($element.data('masonry'))){
	        		
	        		 $element.masonry( {itemSelector: '.masonryitem',gutter: 5,isInitLayout: false});
	        			
	        	}
	    		$('#columns > figure').each(function () {
	 				
	 	 		    $(this).animate({ opacity: 1 });
	 			});
	    		
	    		$element.masonry( 'reloadItems' );
	 			$element.masonry( 'layout' );
	 			
	    		
	 			
	 		 });
			 
	      }
	    };
	 
	 
	 ko.showMoreLess = function(initialText) {
		 
		  
		    var observable = ko.observable(initialText);
		    observable.limit = ko.observable(100);
		    observable.showAll = ko.observable(false);
		    observable.showButton = ko.computed(function() {
		        return observable().length > observable.limit();
		    });
		    observable.toggleShowAll = function() {
		        observable.showAll(!observable.showAll());
		    };
		    observable.display = ko.computed(function() {
		        if (observable.showAll() || !observable.showButton()) { return observable(); }
		        return observable().slice(0,observable.limit());
		    }, observable);
		    return observable;
		};
	 
	 function Citem(data) {
			var self = this;
			
			
			self.id = ko.observable(false);
			self.title = ko.observable(false);
			self.description=ko.observable(false);
			self.thumb = ko.observable(false);
			self.fullres=ko.observable(false);
			self.view_url=ko.observable(false);
			self.source=ko.observable(false);
			self.creator=ko.observable("");
			self.provider=ko.observable("");
			self.url=ko.observable("");
			self.id=ko.observable("");
			
			
			self.load = function(data) {
				if(data.title==undefined){
					self.title("No title");
				}else{self.title(data.title);}
				self.url("#item/"+data.id);
				self.view_url(data.view_url);
				self.thumb(data.thumb);
				self.fullres(data.fullres);
				self.description(data.description);
				self.source(data.source);
				self.creator(data.creator);
				self.provider(data.provider);
				self.id(data.id);
			};

			self.displayTitle = ko.computed(function() {
				if(self.title != undefined) return self.title;
				else if(self.description != undefined) return self.description;
				else return "- No title -";
			});

			if(data != undefined) self.load(data);
		}
	 
	 
	
   function CViewModel(params) {
	  var self = this;

	  
	  self.route = params.route;
	  
	  self.collname=ko.observable('');
	  self.id=ko.observable(params.id);
	  self.owner=ko.observable('');
	  self.ownerId=ko.observable(-1);
	  self.itemCount=ko.observable(0);
	  self.citems = ko.observableArray([]);
  
	  self.description=ko.observable('');
	  self.selectedRecord=ko.observable(false);
		
	  self.loading = ko.observable(false);
	  
	  self.next = ko.observable(-1);
	  self.desc=ko.showMoreLess('');
	  
	  self.loadCollection=function(id){
		 
	    self.loading(true);
		  self.citems([]);
		  $.ajax({
				"url": "/collection/"+self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function(data) {
					console.log(data);
					self.loading(false);
					self.collname(data.title);
					self.desc(data.description);
					self.owner(data.owner);
					self.ownerId(data.ownerId);
					self.itemCount(data.itemCount);
					var items = [];
					for(var i in data.firstEntries){
					 var result = data.firstEntries[i];
					 
					 
					 var record = new Citem({
						id: result.dbId,
						thumb: result.thumbnailUrl,
						title: result.title,
						view_url: result.sourceUrl,
						creator: result.creator!==undefined && result.creator!==null && result.creator[0]!==undefined? result.creator[0].value : "",
						provider: result.dataProvider!=undefined && result.dataProvider!==null && result.dataProvider[0]!==undefined? result.dataProvider[0].value : "",
						source: result.source
					  });
					 items.push(record);}
					
					if(data.firstEntries.length==20){$(window).bind('scroll', scrollHandler);}
					
					self.citems.push.apply(self.citems, items);
					
				},
				
				 error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					
					$("#myModal").find("h4").html("An error occured");
					$("#myModal").find("div.modal-body").html(errorThrown);
			       
					$("#myModal").modal('show');
			     }});
	  }
	  
	  self.loadCollection();
	  self.isOwner =ko.pureComputed(function() {
			if(currentUser !== undefined && currentUser._id()==self.ownerId()){
				return true;
			}
			else{
				return false;
			}
		});

	  
	
	  /*for testing "_id": ObjectId("553df544d4c67f0ff6392667"),*/
	  self.loadNext = function() {
			if(self.citems().length>=20){	
				
				self.moreItems();}
			};

	 		
			
			
	 self.moreItems=function(){
		
		 self.loading(true);
		 var offset=self.citems().length+1;
		 $.ajax({
				"url": "/collection/"+self.id()+"/list?count=20&start="+offset,
				"method": "get",
				"contentType": "application/json",
				"success": function(data) {
					console.log(data);
					self.loading(false);
					
					var items = [];
					for(var i in data){
					 var result = data[i];
					 
					 
					 var record = new Citem({
						id: result.dbId,
						thumb: result.thumbnailUrl,
						title: result.title,
						view_url: result.sourceUrl,
						creator: result.creator!==undefined && result.creator!==null && result.creator[0]!==undefined? result.creator[0].value : "",
						provider: result.dataProvider!=undefined && result.dataProvider!==null && result.dataProvider[0]!==undefined? result.dataProvider[0].value : "",
						source: result.source
					  });
					 items.push(record);}
					
					if(data.length==20){$(window).bind('scroll', scrollHandler);}
					
					self.citems.push.apply(self.citems, items);
					
				},
				
				"error":function(result) {
					self.loading(false);
					$(window).bind('scroll', scrollHandler);
				
			       
					 
			     }});
		
	 }		

	 
	 self.recordSelect= function (e){
			console.log(e);
			
			itemShow(e);
			
		}
	 
	 self.removeRecord= function (e){  
		$("#myModal").find("h4").html("Delete item");
		$("#myModal").find("div.modal-body").html("Are you sure you want to proceed?");
		$("#myModal").find("div.modal-body").append('<div class="modal-footer"><button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button><a class="btn btn-danger btn-ok">Delete</a></div')
		
		$("#myModal").modal('show');
		$('.btn-danger').on('click', function(event) { 
			$("#myModal").remove("div.modal-footer");
			console.log(e);
			 var jsondata=JSON.stringify({
					recId: e.id()
				});
			$.ajax({
                url: '/collection/'+self.id()+'/removeRecord?recId='+e.id(),
                type: 'DELETE',
                contentType: "application/json",
				data:jsondata,
                success: function (data, textStatus, xhr) {
                    console.log(data);
                    self.citems.remove(e);
                    $("#myModal").find("h4").html("Done!");
					$("#myModal").find("div.modal-body").html("Item removed from collection");
			       
					$("#myModal").modal('show');
                },
                error: function (xhr, textStatus, errorThrown) {
                    $("#myModal").find("h4").html("An error occured");
					$("#myModal").find("div.modal-body").html(errorThrown);
					$("#myModal").modal('show');
                }
            });
		});	 
		
		}
	 
	 $(window).scroll(scrollHandler());
	 
	 function scrollHandler(){
		 if (self.loading()==false && $(window).scrollTop() >= $(document).height() - $(window).height() - 10) {
			 $(window).unbind('scroll');/*prevent from firing continuously*/
			
			 self.loadNext();
		   }
	 }
	 
	
		
		
  }



  return { viewModel: CViewModel, template: template };
});
