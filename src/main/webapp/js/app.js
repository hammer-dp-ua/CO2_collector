var CHART;

function getOptions() {
   return {
      title: {
         text: 'CO2 value by minute'
      },

      subtitle: {
         text: 'Current value: 0ppm'
      },

      xAxis: {
         gapGridLineWidth: 0
      },
      yAxis: {
         title: {
            text: 'CO2 (ppm)'
         },
         gridLineWidth: 1
      },

      rangeSelector: {
         buttons: [{
            type: 'hour',
            count: 1,
            text: '1h'
         }, {
            type: 'day',
            count: 1,
            text: '1d'
         }, {
            type: 'week',
            count: 1,
            text: '1w'
         }, {
            type: 'month',
            count: 1,
            text: '1m'
         }, {
            type: 'month',
            count: 6,
            text: '6m'
         }, {
            type: 'all',
            count: 1,
            text: 'All'
         }],

         selected: 1,
         inputEnabled: false
      },

      series: [{
         name: 'ppm',
         type: 'area',
         gapSize: 5,
         tooltip: {
            valueDecimals: 0
         },
         fillColor: {
            linearGradient: {
               x1: 0,
               y1: 0,
               x2: 0,
               y2: 1
            },
            stops: [
               [0, Highcharts.getOptions().colors[0]],
               [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
            ]
         },

         threshold: null,

         zones: [{
            value: 1000
         }, {
            value: 2000,
            color: '#ffa500'
         }, {
            color: '#ff0000'
         }],

         chart: {
            zoomType: "x"
         }
      }],

      chart: {
         events: {
            load: requestLastValue
         }
      }
   };
}

function createChart() {
   // https://cdn.rawgit.com/highcharts/highcharts/v6.0.4/samples/data/new-intraday.json
   var initialDataUrlCookie = $.cookie("initialDataUrl");

   if (!initialDataUrlCookie) {
      initialDataUrlCookie = "/rest/getForThePeriod?period=ALL";
   }

   var options = getOptions();

   $.getJSON(initialDataUrlCookie, function(data) {
      options.series[0].data = data;

      if (data.length > 0) {
         var lastElement = data[data.length - 1];
         var lastElementValue = (lastElement && lastElement.length === 2) ? lastElement[1] : null;

         options.subtitle.text = options.subtitle.text.replace(/\d+/, lastElementValue);
      }
      // create the chart
      CHART = new Highcharts.stockChart('container', options);
   });
}

function requestLastValue() {
   var serie = chart.series[0];
   var currentData = serie.data;
   var lastDataElement = (serie && currentData && currentData.length > 0) ? currentData[currentData.length - 1] : null;
   var lastValueTimestamp = (lastDataElement && typeof lastDataElement[1] === "number") ? lastDataElement[1] : -1;

   $.ajax({
      url: "/getLastValueDeferred?lastValueTimestamp=" + lastValueTimestamp,
      success: function(point) {
         serie.addPoint(point, true, shift);
         setTimeout(requestLastValue, 1000);
      },
      cache: false
   });
}

function recreateChart() {
   if (CHART) {
      CHART.destroy();
   }
   createChart();
}

function setInitialDataUrl(url) {
   $.cookie("initialDataUrl", url);
}

function resetInitialDataUrl() {
   $.removeCookie("initialDataUrl");
}