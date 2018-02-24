var CHART;

function getOptions() {
   return {
      title: {
         text: 'CO2 value by minute'
      },

      subtitle: {
         text: 'Current value: 0ppm; read at 00:00:00'
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
         }]
      }],

      chart: {
         zoomType: "x",
         events: {
            load: function() {
               setTimeout(requestLastValue, 5000);
            }
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
         var lastElementTimestamp = (lastElement && lastElement.length === 2) ? lastElement[0] : null;

         options.subtitle.text = updateSubtitle(options.subtitle.text, lastElementValue,
            new Date(lastElementTimestamp + (new Date().getTimezoneOffset() * 60000)));
      }
      // create the chart
      CHART = new Highcharts.stockChart('container', options);
   });
}

function requestLastValue() {
   if (!CHART || !CHART.series) {
      return;
   }

   var serie = CHART.series[0];
   var currentData = serie.data;
   var lastDataElement = (serie && currentData && currentData.length > 0) ? currentData[currentData.length - 1] : null;
   var lastValueTimestamp = (lastDataElement && typeof lastDataElement.x === "number") ? lastDataElement.x : -1;

   $.ajax({
      url: "/rest/getLastValueDeferred?lastValueTimestamp=" + lastValueTimestamp,
      success: function(point, textStatus, jqXHR) {
         if (!point && point.length !== 2) {
            throw new Error("Illegal point: " + point);
         }

         serie.addPoint(point, true, false);

         var timestamp = point[0];
         var value = point[1];
         CHART.setSubtitle({text: updateSubtitle(CHART.subtitle.textStr, value,
               new Date(timestamp + (new Date().getTimezoneOffset() * 60000)))});
      },
      complete: function(jqXHR, textStatus) {
         setTimeout(requestLastValue, 90000);
      },
      timeout: 60000,
      cache: false
   });
}

function updateSubtitle(currentText, value, dateObj) {
   if (!currentText) {
      return "Current text was empty";
   }
   if (!value || value < 0 || typeof value !== "number") {
      return "Current value was illegal: " + value;
   }
   if (!(dateObj instanceof Date)) {
      return "A date wasn't Date object: " + dateObj;
   }

   var hours = addZero(dateObj.getHours());
   var minutes = addZero(dateObj.getMinutes());
   var seconds = addZero(dateObj.getSeconds());
   var result = currentText.replace(/\d+/, value);

   result = result.replace(/\d\d:/, hours + ":");
   result = result.replace(/:\d\d:/, ":" + minutes + ":");
   result = result.replace(/:\d\d$/, ":" + seconds);
   return result;

   function addZero(value) {
      if (value < 10) {
         return "0" + value;
      } else {
         return value;
      }
   }
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

function SenseAirCommand() {
   var address;
   var functionCode;
   // 16 bits
   var startingAddress;
   // 16 bits
   var data;
}