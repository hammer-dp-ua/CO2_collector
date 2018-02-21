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
         }
         ,
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
         selected:
            1,
         inputEnabled:
            false
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
         }]
      }]
   };
}

var CHART;

function createChart() {
   // https://cdn.rawgit.com/highcharts/highcharts/v6.0.4/samples/data/new-intraday.json
   var initialDataUrlCookie = $.cookie("initialDataUrl");

   if (!initialDataUrlCookie) {
      initialDataUrlCookie = "getForThePeriod?period=ALL";
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