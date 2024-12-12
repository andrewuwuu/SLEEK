# Machine Learning part in SLEEK
SLEEK or Smart Lifestyle Eating for Efficient Kalories is a mobile-based application designed to promote better health by offering personalized lifestyle habit guidance through advanced health data analysis. Our app provides daily meal recommendations tailored to individual preferences and nutritional needs.

## Dataset
[Dataset](https://www.kaggle.com/datasets/kukuroo3/body-performance-data) that we used were based on the physical fitness measurement for male and female managed by the Seoul Olympic Olympic Memorial National Sports Promotion Foundation. Check for more detail about the dataset by visiting their [website](https://www.bigdata-culture.kr/bigdata/user/data_market/detail.do?id=ace0aea7-5eee-48b9-b616-637365d665c1).

## Models
We utilized machine learning models to analyze health performance of human. This analysis created through two models, classification and regression model.

**Classification Model**: To classify users into four different body shapes, we choose this model as a foundation for the next feature.

*Model Accuracy & Loss*

`accuracy: 0.9892 - loss: 0.0369 - val_accuracy: 0.9918 - val_loss: 0.0253`

**Regression Model**: We build regression model to predict the calorie intake of users which based on various factors and parameters.

*Model Accuracy & Loss*

`loss: 0.0839 - mean_absolute_error: 0.1148 - val_loss: 0.1454 - val_mean_absolute_error: 0.2323`

**Generative AI Model**: We conduct a prompt engineering in the Generative AI model to create an elaborative yet informative result for meal recommendations.

## Installation
To run this project, make sure that you have Python 3.9 or higher installed and the following libraries:
* tensorflow
* pandas
* numpy
* scikit-learn
* matplotlib

Or you can just install with the following command:

`pip install --requirements.txt`

## Additional Information
We hope that our machine learning explanation will improve your daily habit and life style. Live a long life!
