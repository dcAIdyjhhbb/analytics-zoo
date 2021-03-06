/*
 * Copyright 2018 Analytics Zoo Authors.
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

package com.intel.analytics.zoo.models.image.imageclassification

import com.intel.analytics.bigdl.nn.SoftMax
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.transform.vision.image.ImageFeature
import com.intel.analytics.zoo.feature.image.ImageProcessing

/**
 * Transform image prediction output to class name labels with probabilities.
 *
 * @param labelMap The map of class ID (Integer) and name (String).
 * @param clses The key to store names of class labels. Default is "classes".
 * @param probs The key to store probabilities of each class. Default is "probs".
 *              The result probability array together with the corresponding class name
 *              array will be sorted by probabilities in descending order.
 * @param probAsOutput Boolean. Whether the prediction output is a probability distribution.
 *                     Default is true. If false, the output will be first applied with SoftMax.
 */
class LabelOutput(labelMap: Map[Int, String], clses: String = "classes",
                  probs: String = "probs", probAsOutput: Boolean = true)
  extends ImageProcessing {
  override def transformMat(imageFeature: ImageFeature): Unit = {
    val predict = imageFeature[Tensor[Float]](ImageFeature.predict)
    val predictOutput = if (! probAsOutput) {
      SoftMax[Float].forward(predict).toTensor[Float]
    } else predict
    val start = predictOutput.storageOffset() - 1
    val end = predictOutput.storageOffset() - 1 + predictOutput.nElement()
    val clsNo = end - start
    val sortedResult = predictOutput.storage().array().slice(start, end).
      zipWithIndex.sortWith(_._1 > _._1).toList.toArray

    val classes: Array[String] = new Array[String](clsNo)
    val probabilities: Array[Float] = new Array[Float](clsNo)

    var index = 0
    while (index < clsNo) {
      val clsName = labelMap(sortedResult(index)._2)
      val prob = sortedResult(index)._1
      classes(index) = clsName
      probabilities(index) = prob
      index += 1
    }

    imageFeature(clses) = classes
    imageFeature(probs) = probabilities
  }
}

object LabelOutput {
  def apply(labelMap: Map[Int, String], classes: String = "classes",
            probs: String = "probs", probAsOutput: Boolean = true): LabelOutput =
    new LabelOutput(labelMap, classes, probs, probAsOutput)
}
