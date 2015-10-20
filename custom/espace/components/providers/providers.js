define(['bridget', 'knockout', 'text!./providers.html','isotope','imagesloaded','app','smoke'], function(bridget,ko, template, Isotope,imagesLoaded,app) {

  
  
  
  $.bridget('isotope', Isotope);
	
  
  
  function randomInt(min,max)
  {
      return Math.floor(Math.random()*(max-min+1)+min);
  }

	 function initOrUpdate(method) {
			return function (element, valueAccessor, allBindings, viewModel, bindingContext) {
				function isotopeAppend(ele) {
					if (ele.nodeType === 1) { // Element type
						$(element).imagesLoaded(function () {
							$(element).isotope('appended', ele).isotope('layout');
						});
					}
				}

				function attachCallback(valueAccessor) {
					return function() {
						return {
							data: valueAccessor(),
							afterAdd: isotopeAppend,
						};
					};
				}

				var data = ko.utils.unwrapObservable(valueAccessor());
				//extend foreach binding
				ko.bindingHandlers.foreach[method](element,
					 attachCallback(valueAccessor), // attach 'afterAdd' callback
					 allBindings, viewModel, bindingContext);

				if (method === 'init') {
					$(element).isotope({
						itemSelector: '.item',
						transitionDuration: transDuration,
						masonry: {
							columnWidth		: '.sizer',
							percentPosition	: true
						}
					});

					ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
						$(element).isotope("destroy");
					});
					
				} 
			};
		}
		
	
		ko.bindingHandlers.providerIsotope = {
				init: initOrUpdate('init'),
				update: initOrUpdate('update')
			};

	

	function Provider(data) {
		var self = this;
	    self.id = "";
		self.name = "";
		self.thumbnail="";
		self.country = "";
		self.background="";
		self.totalCollections=0;
		self.totalExhibitions=0;
		
		self.isLoaded = ko.observable(false);
		 
		self.load = function(data) {
			if(data.title==undefined){
				self.title="No title";
			}else{self.title=data.title;}
			self.url="#provider/"+data.dbId;
			self.thumbnail='/media/' + data.thumbnail;
			self.country=data.page.country;
			self.totalCollections=data.totalCollections;
			self.totalExhibitions=data.totalExhibitions;
			self.name=data.friendlyName !=null? data.friendlyName : data.username;
			self.background='/media/' + data.page.coverThumbnail;
			
		};

		
		if(data != undefined) self.load(data);
		
	}

	function ProvidersViewModel(params) {
		this.route = params.route;
	    document.body.setAttribute("data-page","contentproviders");
		
		var self = this;

		var $container = $(".grid");
		self.route = params.route;
		var counter = 1;
		self.collname = ko.observable('');
		self.access = ko.observable("READ");
		self.id = ko.observable(params.id);
		self.owner = ko.observable('');
		self.ownerId = ko.observable(-1);
		self.itemCount = ko.observable(0);
		self.providers = ko.observableArray();

		
	   
		self.description = ko.observable('');
		self.selectedRecord = ko.observable(false);

		self.loading = ko.observable(false);

		
		self.revealItems = function (data) {
			for (var i in data) {
				var result = data[i];
				var record = new Provider(data[i]);
				
				self.providers().push(record);
			}
			self.providers.valueHasMutated();
		};
		
	

		self.loadProviders = function () {
			
			// replace with group/descendantOrganizations/:projectId
			self.loading(true);
			$.ajax({
				"url": "/group/descendantOrganizations/" + projectId+"?collectionHits=true",
				"method": "get",
				"contentType": "application/json",
				"success": function (data) {
					
					self.revealItems(data);
					self.loading(false);
					window.EUSpaceUI.initTooltip();
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};

		self.loadProviders();
		

		
	}

	return { viewModel: ProvidersViewModel, template: template };
});