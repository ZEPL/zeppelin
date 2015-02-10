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

  interface IParagraphChartCtrlScope extends ng.IScope {

    $parent: IParagraphCtrlScope;
  }

  angular.module('zeppelinWebApp').controller('ParagraphChartCtrl', function(
    $scope: IParagraphChartCtrlScope,
    $rootScope: IZeppelinRootScope,
    $timeout: ng.ITimeoutService) {

  });
}
