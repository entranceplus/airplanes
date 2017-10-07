(ns airplanes.pipeline
  (:use [lambdacd.steps.control-flow]
        [airplanes.steps :as steps])
  (:require
        [lambdacd.steps.manualtrigger :as manualtrigger]))

(def demo-pipeline-def
  `(
    manualtrigger/wait-for-manual-trigger
    some-step-that-does-nothing
    some-step-that-echos-bar
    manualtrigger/wait-for-manual-trigger
    some-failing-step))

(def other-pipeline-def
  `(
    manualtrigger/wait-for-manual-trigger
    some-step-that-does-nothing
    some-step-that-echos-bar))
