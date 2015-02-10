/* Copyright 2014 NFLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/**
 * @ngdoc function
 * @name zeppelinWebApp.controller:ParagraphEditorCtrl
 * @description
 * # ParagraphEditorCtrl
 * Controller of the paragraph editor
 */

module zeppelin {

  interface IParagraphResultCtrlScope extends ng.IScope {
    init: () => void;

    chart: any;

    setGraphMode: (type: string) => void;
    drawGraph: (refresh?: boolean) => void;
    renderHtml: () => void;
    loadTableData: (result: any) => void;

    onGraphOptionChange: () => void;
    removeGraphOptionKeys: (idx: number) => void;
    removeGraphOptionValues: (idx: number) => void;
    removeGraphOptionGroups: (idx: number) => void;
    setGraphOptionValueAggr: (idx: number, aggr: any) => void;

    d3: any;

    $parent: IParagraphCtrlScope;
  }

  angular.module('zeppelinWebApp').controller('ParagraphResultCtrl', function(
    $scope: IParagraphResultCtrlScope,
    $rootScope: IZeppelinRootScope,
    $timeout: ng.ITimeoutService) {

    // Controller init
    $scope.init = function() {
      $scope.chart = {};

      if ($scope.$parent.paragraph.resultType() === PResultType.TABLE) {
        $scope.$parent.lastData.settings = angular.copy($scope.$parent.paragraph.settings);
        $scope.$parent.lastData.config = angular.copy($scope.$parent.paragraph.config);
        $scope.loadTableData($scope.$parent.paragraph.result);
        $scope.drawGraph();
      } else if ($scope.$parent.paragraph.resultType() === PResultType.HTML) {
        $scope.renderHtml();
      }
    };

    $scope.$on('updateParagraph', function(event, data) {
      var updatedParagraph = new Paragraph(data.paragraph);

      if (updatedParagraph.id !== $scope.$parent.paragraph.id) return;

      var oldType = $scope.$parent.paragraph.resultType();
      var newType = updatedParagraph.resultType();
      var oldGraphMode = $scope.$parent.paragraph.graphMode();
      var newGraphMode = updatedParagraph.graphMode();
      var resultRefreshed = (updatedParagraph.dateFinished !== $scope.$parent.paragraph.dateFinished);


      if (newType === PResultType.TABLE) {
        $scope.loadTableData($scope.$parent.paragraph.result);
        if (oldType !== PResultType.TABLE || resultRefreshed) {
          clearUnknownColsFromGraphOption();
          selectDefaultColsForGraphOption();
        }
        /** User changed the chart type? */
        if (oldGraphMode !== newGraphMode) {
          $scope.setGraphMode(newGraphMode);
        } else {
          $scope.drawGraph();
        }
      } else if (newType === PResultType.HTML) {
        $scope.renderHtml();
      }
    });

    $scope.renderHtml = function() {
      var retryRenderer = function() {
        if ($('#p' + $scope.$parent.paragraph.id + '_html').length) {
          try {
            $('#p' + $scope.$parent.paragraph.id + '_html').html($scope.$parent.paragraph.result.msg);
          } catch(err) {
            console.log('HTML rendering error %o', err);
          }
        } else {
          $timeout(retryRenderer, 10);
        }
      };
      $timeout(retryRenderer);
    };

    $scope.loadTableData = function(result) {
      if (!result) {
        return;
      }
      if (result.type === PResultType.TABLE) {
        var columnNames = [];
        var rows = [];
        var array = [];
        var textRows = result.msg.split('\n');
        result.comment = '';
        var comment = false;

        for (var i = 0; i < textRows.length; i++) {
          var textRow = textRows[i];
          if (comment) {
            result.comment += textRow;
            continue;
          }

          if (textRow === '') {
            if (rows.length > 0) {
              comment = true;
            }
            continue;
          }
          var textCols = textRow.split('\t');
          var cols = [];
          var cols2 = [];
          for (var j = 0; j < textCols.length; j++) {
            var col = textCols[j];
            if (i === 0) {
              columnNames.push({name: col, index: j, aggr: 'sum'});
            } else {
              cols.push(col);
              cols2.push({key: (columnNames[i]) ? columnNames[i].name: undefined, value: col});
            }
          }
          if (i !== 0) {
            rows.push(cols);
            array.push(cols2);
          }
        }
        result.msgTable = array;
        result.columnNames = columnNames;
        result.rows = rows;
      }
    };

    $scope.setGraphMode = function(type) {
      $scope.$parent.paragraph.config.graph.mode = type;
      $scope.$parent.commitParagraph();
      $scope.drawGraph();
    };

    $scope.drawGraph = function(refresh?: boolean) {
      var refresh = refresh ? refresh : true;
      var type = $scope.$parent.paragraph.config.graph.mode;

      clearUnknownColsFromGraphOption();
      // set graph height
      var height = $scope.$parent.paragraph.config.graph.height;
      $('#p' + $scope.$parent.paragraph.id + '_graph').height(height);

      if (!type || type === GraphMode.table) {
        setTable($scope.$parent.paragraph.result, refresh);
      }
      else {
        setD3Chart(type, $scope.$parent.paragraph.result, refresh);
      }
    }

    var setTable = function(data, refresh) {
      var getTableContentFormat = function(d) {
        if (isNaN(d)) {
          if (d.length > '%html'.length && '%html ' === d.substring(0, '%html '.length)) {
            return 'html';
          } else {
            return '';
          }
        } else {
          return '';
        }
      };

      var formatTableContent = function(d) {
        if (isNaN(d)) {
          var f = getTableContentFormat(d);
          if (f !== '') {
            return d.substring(f.length + 2);
          } else {
            return d;
          }
        } else {
          var dStr = d.toString();
          var splitted = dStr.split('.');
          var formatted = splitted[0].replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
          if (splitted.length>1) {
            formatted += '.' + splitted[1];
          }
          return formatted;
        }
      };

      var renderTable = function() {
        var html = '';
        html += '<table class=\'table table-hover table-condensed\'>';
        html += '  <thead>';
        html += '    <tr style=\'background-color: #F6F6F6; font-weight: bold;\'>';
        for (var c in $scope.$parent.paragraph.result.columnNames) {
          html += '<th>' + $scope.$parent.paragraph.result.columnNames[c].name + '</th>';
        }
        html += '    </tr>';
        html += '  </thead>';

        for (var r in $scope.$parent.paragraph.result.msgTable) {
          var row = $scope.$parent.paragraph.result.msgTable[r];
          html += '    <tr>';
          for (var index in row) {
            var v = row[index].value;
            if (getTableContentFormat(v) !== 'html') {
              v = v.replace(/[\u00A0-\u9999<>\&]/gim, function(i) {
                return '&#' + i.charCodeAt(0) + ';';
              });
            }
            html += '      <td>' + formatTableContent(v) + '</td>';
          }
          html += '    </tr>';
        }
        html += '</table>';

        $('#p' + $scope.$parent.paragraph.id + '_table').html(html);
        $('#p' + $scope.$parent.paragraph.id + '_table').perfectScrollbar();

        // set table height
        var height = $scope.$parent.paragraph.config.graph.height;
        $('#p' + $scope.$parent.paragraph.id + '_table').height(height);
      };

      var retryRenderer = function() {
        if ($('#p' + $scope.$parent.paragraph.id + '_table').length) {
          try {
            renderTable();
          } catch(err) {
            console.log('Chart drawing error %o', err);
          }
        } else {
          $timeout(retryRenderer,10);
        }
      };
      $timeout(retryRenderer);
    };

    var setD3Chart = function(type, data, refresh) {
      if (!$scope.chart[type]) {
        var chart = nv.models[type]();
        $scope.chart[type] = chart;
      }

      var p = pivot(data);

      var xColIndexes = $scope.$parent.paragraph.config.graph.keys;
      var yColIndexes = $scope.$parent.paragraph.config.graph.values;

      var d3g = [];
      // select yColumns.

      if (type === GraphMode.pieChart) {
        var d = pivotDataToD3ChartFormat(p, true).d3g;

        $scope.chart[type].x(function(d) { return d.label;})
          .y(function(d) { return d.value;});

        if ( d.length > 0 ) {
          for ( var i=0; i<d[0].values.length ; i++) {
            var e = d[0].values[i];
            d3g.push({
              label : e.x,
              value : e.y
            });
          }
        }
      } else if (type === GraphMode.multiBarChart) {
        d3g = pivotDataToD3ChartFormat(p, true, false, type).d3g;
        $scope.chart[type].yAxis.axisLabelDistance(50);
      } else {
        var pivotdata = pivotDataToD3ChartFormat(p, false, true);
        var xLabels = pivotdata.xLabels;
        d3g = pivotdata.d3g;
        $scope.chart[type].xAxis.tickFormat(function(d) {
          if (xLabels[d] && (isNaN(parseFloat(xLabels[d])) || !isFinite(xLabels[d]))) { // to handle string type xlabel
            return xLabels[d];
          } else {
            return d;
          }
        });
        $scope.chart[type].yAxis.axisLabelDistance(50);
        $scope.chart[type].useInteractiveGuideline(true); // for better UX and performance issue. (https://github.com/novus/nvd3/issues/691)
        $scope.chart[type].forceY([0]); // force y-axis minimum to 0 for line chart.
      }

      var renderChart = function() {
        if (!refresh) {
          // TODO force destroy previous chart
        }

        var height = $scope.$parent.paragraph.config.graph.height;

        var animationDuration = 300;
        var numberOfDataThreshold = 150;
        // turn off animation when dataset is too large. (for performance issue)
        // still, since dataset is large, the chart content sequentially appears like animated.
        try {
          if (d3g[0].values.length > numberOfDataThreshold) {
            animationDuration = 0;
          }
        } catch(ignoreErr) {
        }

        var chartEl = d3.select('#p' + $scope.$parent.paragraph.id + '_' + type + ' svg')
          .attr('height', $scope.$parent.paragraph.config.graph.height)
          .datum(d3g)
          .transition()
          .duration(animationDuration)
          .call($scope.chart[type]);
        d3.select('#p' + $scope.$parent.paragraph.id + '_' + type + ' svg').style('height', height + 'px');
        nv.utils.windowResize($scope.chart[type].update);
      };

      var retryRenderer = function() {
        if ($('#p' + $scope.$parent.paragraph.id + '_' + type + ' svg').length !== 0) {
          try {
            renderChart();
          } catch(err) {
            console.log('Chart drawing error %o', err);
          }
        } else {
          $timeout(retryRenderer,10);
        }
      };
      $timeout(retryRenderer);
    };

    var setPieChart = function(data, refresh) {
      var xColIndex = 0;
      var yColIndexes = [];
      var d3g = [];

      // select yColumns.
      for (var colIndex = 0; colIndex < data.columnNames.length; colIndex++) {
        if (colIndex !== xColIndex) {
          yColIndexes.push(colIndex);
        }
      }

      for (var rowIndex = 0; rowIndex < data.rows.length; rowIndex++) {
        var row = data.rows[rowIndex];
        var xVar = row[xColIndex];
        var yVar = row[yColIndexes[0]];

        d3g.push({
          label: isNaN(xVar) ? xVar : parseFloat(xVar),
          value: parseFloat(yVar)
        });
      }

      if ($scope.d3.pieChart.data === null || !refresh) {
        $scope.d3.pieChart.data = d3g;
        $scope.d3.pieChart.options.chart.height = $scope.$parent.paragraph.config.graph.height;

        if ($scope.d3.pieChart.api) {
          $scope.d3.pieChart.api.updateWithOptions($scope.d3.pieChart.options);
        }
      } else {
        if ($scope.d3.pieChart.api) {
          $scope.d3.pieChart.api.updateWithData(d3g);
        }
      }
    };

    $scope.onGraphOptionChange = function() {
      $scope.drawGraph();
      $scope.$parent.commitParagraph();
    };

    $scope.removeGraphOptionKeys = function(idx) {
      $scope.$parent.paragraph.config.graph.keys.splice(idx, 1);
      $scope.onGraphOptionChange();
    };

    $scope.removeGraphOptionValues = function(idx) {
      $scope.$parent.paragraph.config.graph.values.splice(idx, 1);
      $scope.onGraphOptionChange();
    };

    $scope.removeGraphOptionGroups = function(idx) {
      $scope.$parent.paragraph.config.graph.groups.splice(idx, 1);
      $scope.onGraphOptionChange();
    };

    $scope.setGraphOptionValueAggr = function(idx, aggr) {
      $scope.$parent.paragraph.config.graph.values[idx].aggr = aggr;
      $scope.onGraphOptionChange();
    };

    /* Clear unknown columns from graph option */
    var clearUnknownColsFromGraphOption = function() {
      var graph = $scope.$parent.paragraph.config.graph;

      var unique = function(list) {
        for (var i = 0; i < list.length; i++) {
          for (var j = i + 1; j < list.length; j++) {
            if (angular.equals(list[i], list[j])) {
              list.splice(j, 1);
            }
          }
        }
      };

      var removeUnknown = function(list) {
        for (var i = 0; i < list.length; i++) {
          // remove non existing column
          var found = false;
          for (var j = 0; j < $scope.$parent.paragraph.result.columnNames.length; j++) {
            var a = list[i];
            var b = $scope.$parent.paragraph.result.columnNames[j];
            if (a.index === b.index && a.name === b.name) {
              found = true;
              break;
            }
          }
          if (!found) {
            list.splice(i, 1);
          }
        }
      };

      unique(graph.keys);
      removeUnknown(graph.keys);

      removeUnknown(graph.values);

      unique(graph.groups);
      removeUnknown(graph.groups);
    };

    var pivot = function(data) {
      var graph = $scope.$parent.paragraph.config.graph;
      var keys = graph.keys;
      var groups = graph.groups;
      var values = graph.values;

      var aggrFunc = {
        sum : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return varA + varB;
        },
        count : function(a,b) {
          var varA = (a !== undefined) ? parseInt(a) : 0;
          var varB = (b !== undefined) ? 1 : 0;
          return varA + varB;
        },
        min : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return Math.min(varA,varB);
        },
        max : function(a,b) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return Math.max(varA,varB);
        },
        avg : function(a,b,c) {
          var varA = (a !== undefined) ? (isNaN(a) ? 1 : parseFloat(a)) : 0;
          var varB = (b !== undefined) ? (isNaN(b) ? 1 : parseFloat(b)) : 0;
          return varA + varB;
        }
      };

      var aggrFuncDiv = {
        sum : false,
        count : false,
        min : false,
        max : false,
        avg : true
      };

      var schema = {};
      var rows = {};

      for (var i=0; i < data.rows.length; i++) {
        var row = data.rows[i];
        var newRow = {};
        var s = schema;
        var p = rows;

        for (var k=0; k < keys.length; k++) {
          var key = keys[k];

          // add key to schema
          if (!s[key.name]) {
            s[key.name] = {
              order : k,
              index : key.index,
              type : 'key',
              children : {}
            };
          }
          s = s[key.name].children;

          // add key to row
          var keyKey = row[key.index];
          if (!p[keyKey]) {
            p[keyKey] = {};
          }
          p = p[keyKey];
        }

        for (var g=0; g < groups.length; g++) {
          var group = groups[g];
          var groupKey = row[group.index];

          // add group to schema
          if (!s[groupKey]) {
            s[groupKey] = {
              order : g,
              index : group.index,
              type : 'group',
              children : {}
            };
          }
          s = s[groupKey].children;

          // add key to row
          if (!p[groupKey]) {
            p[groupKey] = {};
          }
          p = p[groupKey];
        }

        for (var v=0; v < values.length; v++) {
          var value = values[v];
          var valueKey = value.name + '(' + value.aggr + ')';

          // add value to schema
          if (!s[valueKey]) {
            s[valueKey] = {
              type : 'value',
              order : v,
              index : value.index
            };
          }

          // add value to row
          if (!p[valueKey]) {
            p[valueKey] = {
              value : (value.aggr !== 'count') ? row[value.index] : 1,
              count: 1
            };
          } else {
            p[valueKey] = {
              value : aggrFunc[value.aggr](p[valueKey].value, row[value.index], p[valueKey].count + 1),
              count : (aggrFuncDiv[value.aggr]) ?  p[valueKey].count + 1 : p[valueKey].count
            };
          }
        }
      }
      //console.log('schema=%o, rows=%o', schema, rows);

      return {
        schema : schema,
        rows : rows
      };
    };

    var pivotDataToD3ChartFormat = function(data, allowTextXAxis, fillMissingValues?, chartType?) {
      // construct d3 data
      var d3g = [];

      var schema = data.schema;
      var rows = data.rows;
      var values = $scope.$parent.paragraph.config.graph.values;

      var concat = function(o, n) {
        if (!o) {
          return n;
        } else {
          return o + '.' + n;
        }
      };

      var getSchemaUnderKey = function(key, s) {
        for (var c in key.children) {
          s[c] = {};
          getSchemaUnderKey(key.children[c], s[c]);
        }
      };

      var traverse = function(sKey, s, rKey, r, func, rowName?, rowValue?, colName?) {
        //console.log('TRAVERSE sKey=%o, s=%o, rKey=%o, r=%o, rowName=%o, rowValue=%o, colName=%o', sKey, s, rKey, r, rowName, rowValue, colName);

        if (s.type === 'key') {
          rowName = concat(rowName, sKey);
          rowValue = concat(rowValue, rKey);
        } else if (s.type === 'group') {
          colName = concat(colName, rKey);
        } else if (s.type === 'value' && sKey === rKey || valueOnly) {
          colName = concat(colName, rKey);
          func(rowName, rowValue, colName, r);
        }

        for (var c in s.children) {
          if (fillMissingValues && s.children[c].type === 'group' && r[c] === undefined) {
            var cs = {};
            getSchemaUnderKey(s.children[c], cs);
            traverse(c, s.children[c], c, cs, func, rowName, rowValue, colName);
            continue;
          }

          for (var j in r) {
            if (s.children[c].type === 'key' || c === j) {
              traverse(c, s.children[c], j, r[j], func, rowName, rowValue, colName);
            }
          }
        }
      };

      var graph = $scope.$parent.paragraph.config.graph;
      var keys = graph.keys;
      var groups = graph.groups;
      var values = graph.values;
      var valueOnly = (keys.length === 0 && groups.length === 0 && values.length > 0);
      var noKey = (keys.length === 0);
      var isMultiBarChart = (chartType === GraphMode.multiBarChart);

      var sKey = Object.keys(schema)[0];

      var rowNameIndex = {};
      var rowIdx = 0;
      var colNameIndex = {};
      var colIdx = 0;
      var rowIndexValue = {};

      for (var k in rows) {
        traverse(sKey, schema[sKey], k, rows[k], function(rowName, rowValue, colName, value) {
          //console.log('RowName=%o, row=%o, col=%o, value=%o', rowName, rowValue, colName, value);
          if (rowNameIndex[rowValue] === undefined) {
            rowIndexValue[rowIdx] = rowValue;
            rowNameIndex[rowValue] = rowIdx++;
          }

          if (colNameIndex[colName] === undefined) {
            colNameIndex[colName] = colIdx++;
          }
          var i = colNameIndex[colName];
          if (noKey && isMultiBarChart) {
            i = 0;
          }

          if (!d3g[i]) {
            d3g[i] = {
              values : [],
              key : (noKey && isMultiBarChart) ? 'values' : colName
            };
          }

          var xVar = isNaN(rowValue) ? ((allowTextXAxis) ? rowValue : rowNameIndex[rowValue]) : parseFloat(rowValue);
          var yVar = 0;
          if (xVar === undefined) { xVar = colName; }
          if (value !== undefined) {
            yVar = isNaN(value.value) ? 0 : parseFloat(value.value) / parseFloat(value.count);
          }
          d3g[i].values.push({
            x : xVar,
            y : yVar
          });
        });
      }

      // clear aggregation name, if possible
      var namesWithoutAggr = {};
      // TODO - This part could use som refactoring - Weird if/else with similar actions and variable names
      for (var colName in colNameIndex) {
        var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
        if (!namesWithoutAggr[withoutAggr]) {
          namesWithoutAggr[withoutAggr] = 1;
        } else {
          namesWithoutAggr[withoutAggr]++;
        }
      }

      if (valueOnly) {
        for (var valueIndex = 0; valueIndex < d3g[0].values.length; valueIndex++) {
          var colName = d3g[0].values[valueIndex].x;
          if (!colName) {
            continue;
          }

          var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
          if (namesWithoutAggr[withoutAggr] <= 1 ) {
            d3g[0].values[valueIndex].x = withoutAggr;
          }
        }
      } else {
        for (var d3gIndex = 0; d3gIndex < d3g.length; d3gIndex++) {
          var colName = d3g[d3gIndex].key;
          var withoutAggr = colName.substring(0, colName.lastIndexOf('('));
          if (namesWithoutAggr[withoutAggr] <= 1 ) {
            d3g[d3gIndex].key = withoutAggr;
          }
        }

        // use group name instead of group.value as a column name, if there're only one group and one value selected.
        if (groups.length === 1 && values.length === 1) {
          for (d3gIndex = 0; d3gIndex < d3g.length; d3gIndex++) {
            var colName = d3g[d3gIndex].key;
            colName = colName.split('.')[0];
            d3g[d3gIndex].key = colName;
          }
        }
      }

      return {
        xLabels : rowIndexValue,
        d3g : d3g
      };
    };

    /* select default key and value if there're none selected */
    var selectDefaultColsForGraphOption = function() {
      var graph = $scope.$parent.paragraph.config.graph;
      var columnNames = $scope.$parent.paragraph.result.columnNames;

      if (graph.keys.length === 0 && columnNames.length > 0) {
        graph.keys.push(columnNames[0]);
      }

      if (graph.values.length === 0 && columnNames.length > 1) {
        graph.values.push(columnNames[1]);
      }
    };
  });
}
