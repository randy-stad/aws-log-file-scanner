package us.stad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class LambdaFunctionHandler implements RequestHandler<S3Event, Object> {

    @Override
    public Object handleRequest(S3Event input, Context context) {

        // ARN we will publish notifications to

        String topicARN = "arn:aws:sns:us-west-2:668097870300:quantum-support-poc-alert";

        // compile our patterns

        final List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(Pattern.compile("^.*(ERROR|WARNING|FATAL|FAIL).*$"));
        patterns.add(Pattern.compile("^.*(Service smbd stopped).*$"));
        patterns.add(Pattern.compile("^.*(Unable to connect to smbd).*$"));
        patterns.add(Pattern.compile("^.*(hung_task|tainted).*$"));

        // get the bucket and key

        S3EventNotification.S3EventNotificationRecord record = input.getRecords().get(0);
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        context.getLogger().log("bucket: " + bucket + " key: " + key);

        // connect to the bucket and stream the object through our pattern
        // matcher

        AmazonS3 s3Client = new AmazonS3Client();
        S3Object object = s3Client.getObject(new GetObjectRequest(bucket, key));
        List<String> matches = new ArrayList<String>();
        try (InputStream objectData = object.getObjectContent();
                InputStreamReader isr = new InputStreamReader(objectData);
                BufferedReader reader = new BufferedReader(isr);) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                for (Pattern pattern : patterns) {
                    Matcher m = pattern.matcher(line);
                    if (m.find ()) {
                        matches.add(line);
                        context.getLogger().log("match: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // publish SNS message

        if (matches.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Log file alert: ");
            sb.append(bucket);
            sb.append("/");
            sb.append(key);
            sb.append(" found ");
            sb.append(matches.size());
            sb.append(" matches, check the log");
            final AmazonSNSClient snsClient = new AmazonSNSClient();
            snsClient.setRegion(Region.getRegion(Regions.US_WEST_2));
            final PublishRequest publishRequest = new PublishRequest(topicARN, sb.toString());
            PublishResult publishResult = snsClient.publish(publishRequest);
            context.getLogger().log("SNS message id: " + publishResult.getMessageId());
        }

        return null;
    }

}
