# aws-log-file-scanner

A simple Java log file scanner using AWS Lambda that scans incoming S3 objects for sample regex patterns.

## Build

    mvn clean install

## Deploy

The first step to deploy is to grant the correct permissions so Lambda will work with your bucket:

    aws lambda add-permission \
    --function-name aws-log-file-scanner \
    --region us-west-2 \
    --statement-id aws-log-file-scanner-1 \
    --action "lambda:InvokeFunction" \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::<bucket name> \
    --source-account <account id> \
    --profile <aws profile>

The next step is to create the actual Lambda function with the code you just built:

    aws lambda create-function \
    --region us-west-2 \
    --function-name aws-log-file-scanner \
    --zip-file fileb:///<path to src>/aws-log-file-scanner/target/aws-log-file-scanner-1.0-SNAPSHOT.jar \
    --role arn:aws:iam::<IAM role ARN see below> \
    --handler com.quantum.LambdaFunctionHandler::handleRequest \
    --runtime java8 \
    --profile <aws profile> \
    --timeout 30 \
    --memory-size 1024

I was too lazy to write the command to create the IAM role but you can do that through the AWS console and add the **AWSLambdaExecute** and **AmazonSNSFullAccess** policies to the role.
