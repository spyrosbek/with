define(['knockout', 'text!./item.html', 'app'], function (ko, template, app) {

	function Record(data) {
		var self = this;
		self.recordId = ko.observable("");
		self.title = ko.observable(false);
		self.description = ko.observable(false);
		self.thumb = ko.observable(false);
		self.fullres = ko.observable(false);
		self.view_url = ko.observable(false);
		self.source = ko.observable(false);
		self.creator = ko.observable("");
		self.provider = ko.observable("");
		self.rights = ko.observable("");
		self.url = ko.observable("");
		self.id = ko.observable("");
		self.externalId = ko.observable("");
		self.collectedCount = ko.observable("");
		self.liked = ko.observable("");
		self.related =  ko.observableArray([]);
		self.similar =  ko.observableArray([]);
		self.facebook='';
		self.twitter='';
		self.mail='';
		self.forsimilar=ko.observable("").extend({ uppercase: true });
		self.similarlabel='';
		self.loc=ko.observable('');
		
		self.pinterest=function() {
		    var url = encodeURIComponent(self.loc());
		    var media = encodeURIComponent(self.fullres());
		    var desc = encodeURIComponent(self.title()+" on "+window.location.host);
		    window.open("//www.pinterest.com/pin/create/button/"+
		    "?url="+url+
		    "&media="+media+
		    "&description="+desc,'','height=500,width=750');
		    return false;
		};
		 
		
		self.cachedThumbnail = ko.pureComputed(function() {


			   if(self.thumb()){
				return self.thumb();}
			   else{
				   return "img/content/thumb-empty.png";
			   }
			});
		self.load = function (data) {
			if (data.title == undefined) {
				self.title("No title");
			} else {
				self.title(data.title);
			}

			if (data.id) {
				self.recordId(data.id);
				
			} else {
				self.recordId(data.recordId);
			}
			self.loc(location.href.replace(location.hash,"")+"#item/"+self.recordId());
			
			self.url("#item/" + self.recordId());
			self.view_url(data.view_url);
			self.thumb(data.thumb);

			if (data.source!="Rijksmuseum" && data.fullres && data.fullres[0]  && data.fullres[0].length > 0) {
				self.fullres(data.fullres[0]);
			} else {
				self.fullres(self.cachedThumbnail());
			}

			if (data.description == undefined) {
				self.description(data.title);
			} else {
				self.description(data.description);
			}

			if (data.creator !== undefined) {
				self.creator(data.creator);
			}

			if (data.provider !== undefined) {
				self.provider(data.provider);
			}

			if (data.rights !== undefined) {
				self.rights(data.rights);
			}

			self.externalId(data.externalId);
			self.source(data.source);
			self.facebook='https://www.facebook.com/sharer/sharer.php?u='+encodeURIComponent(self.loc());
			self.twitter='https://twitter.com/share?url='+encodeURIComponent(self.loc())+'&text='+encodeURIComponent(self.title()+" on "+window.location.host)+'"';
			self.mail="mailto:?subject="+self.title()+"&body="+encodeURIComponent(self.loc());
			
			
	
			
		};

		self.findsimilar=function(){
		  if(self.similar().length==0){
			self.provider().length>0? self.forsimilar(self.provider().toUpperCase()) : self.forsimilar(self.creator().toUpperCase());
            self.similarlabel=self.provider().length>0? "PROVIDER" : "CREATOR";
            if(self.forsimilar().length>0){
           $.ajax({
				type    : "post",
				url     : "/api/advancedsearch",
				contentType: "application/json",
				data     : JSON.stringify({
					searchTerm: self.forsimilar(),
					page: 1,
					pageSize:10,
				    source:[self.source()],
				    filters:[]
				}),
				success : function(result) {
					data=result.responces[0]!=undefined ? result.responces[0].items :null;
					var items=[];
					if(data!=null) 
						for (var i in data) {
							var result = data[i];
							 if(result !=null){
									
						        var record = new Record({
									recordId: result.recordId || result.id,
									thumb: result.thumb!=null && result.thumb[0]!=null  && result.thumb[0]!="null" ? result.thumb[0]:"",
									fullres: result.fullresolution!=null ? result.fullresolution : "",
									title: result.title!=null? result.title:"",
									view_url: result.url.fromSourceAPI,
									creator: result.creator!==undefined && result.creator!==null? result.creator : "",
									provider: result.dataProvider!=undefined && result.dataProvider!==null ? result.dataProvider: "",
									rights: result.rights!==undefined && result.rights!==null ? result.rights : "",
									externalId: result.externalId,
									source: self.source()
								  });
						        if(record.thumb() && record.thumb().length>0)
							       items.push(record);
							}
							 if(items.length>3){break;}
						}	
					self.similar().push.apply(self.similar(),items);
					self.similar.valueHasMutated();
				},
				error   : function(request, status, error) {
					console.log(request);
				}
			});
            }
			}
            //missing find related
		}
		
		self.sourceImage = ko.pureComputed(function () {
			switch (self.source()) {
			case "DPLA":
				return "images/logos/dpla.png";
			case "Europeana":
				return "images/logos/europeana.jpeg";
			case "NLA":
				return "images/logos/nla_logo.png";
			case "DigitalNZ":
				return "images/logos/digitalnz.png";
			case "EFashion":
				return "images/logos/eufashion.png";
			case "YouTube":
				{
					return "images/logos/youtube.jpg";
				}
			case "WITHin":
				return "images/logos/with_logo.png";
			case "Rijksmuseum":
				return "images/logos/Rijksmuseum.png";
			default:
				return "";
			}
		});

		self.sourceCredits = ko.pureComputed(function () {
			switch (self.source()) {
			case "DPLA":
				return "dpla.eu";
			case "Europeana":
				return "europeana.eu";
			case "NLA":
				return "nla.gov.au";
			case "DigitalNZ":
				return "digitalnz.org";
			case "EFashion":
				return "europeanafashion.eu";
			case "YouTube":
				{
					return "youtube.com";
				}
			case "WITHin":
				return "WITHin";
			default:
				return "";
			}
		});

		self.displayTitle = ko.pureComputed(function () {
			var distitle = "";
			distitle = self.title();
			if (self.creator() !== undefined && self.creator().length > 0)
				distitle += ", by " + self.creator();
			if (self.provider() !== undefined && self.provider().length > 0 && self.provider() != self.creator())
				distitle += ", " + self.provider();
			return distitle;
		});

		if (data !== undefined) self.load(data);
	}

	function ItemViewModel(params) {
		var self = this;
		
		self.route = params.route;
		self.from=window.location.href;	
		var thumb = "";
		self.record = ko.observable(new Record());
		self.id = ko.observable(params.id);
		itemShow = function (e) {
			data = ko.toJS(e);
			$('.nav-tabs a[href="#information"]').tab('show');
			$(".mediathumb > img").attr("src","");
			self.open();
			self.record(new Record(data));
			
		};

		self.open = function () {
			if (window.location.href.indexOf('#item')>0) {
				document.body.setAttribute("data-page","media");	
				
			}
			
		};

		self.close = function () {
			//self.record(new Record());
			$( '.itemview' ).fadeOut();
			
		};

		self.changeSource = function (item) {
			item.record().fullres(item.record().thumb());
		};

		self.collect = function (item) {
			if (!isLogged()) {
				showLoginPopup(self.record());
			} else {
				collectionShow(self.record());
			}
		};

		self.recordSelect = function (e) {
			itemShow(e);
		};
		
		self.loadCollectionnnn = function(collection) {
			window.location.href = 'index.html#collectionview/' + collection.dbId;		
			
			if (isOpen){
				toggleSearch(event,'');
			}
			self.close();
		};
		
		self.loadItem = function () {
			$.ajax({
				"url": "/record/" + self.id(),
				"method": "get",
				"contentType": "application/json",
				"success": function (result) {
					 var record = new Record({
							recordId: result.recordId || result.id,
							thumb: result.thumbnailUrl,
							fullres: result.fullresolution!=null ? result.fullresolution : "",
							title: result.title!=null? result.title:"",
							view_url: result.sourceUrl,
							creator: result.creator!==undefined && result.creator!==null? result.creator : "",
							provider: result.dataProvider!=undefined && result.dataProvider!==null ? result.dataProvider: "",
							rights: result.rights!==undefined && result.rights!==null ? result.rights : "",
							externalId: result.externalId,
							source: result.source
						  });
					self.record(record);
					$('.nav-tabs a[href="#information"]').tab('show');
					$(".mediathumb > img").attr("src","");
					self.open();
					$( '.itemview' ).fadeIn();
					
				},
				error: function (xhr, textStatus, errorThrown) {
					self.loading(false);
					$.smkAlert({text:'An error has occured', type:'danger', permanent: true});
				}
			});
		};
		if(self.id()!=undefined){
			
			self.loadItem();
		}
		
	}
	
	
	return {
		viewModel: ItemViewModel,
		template: template
	};
});
